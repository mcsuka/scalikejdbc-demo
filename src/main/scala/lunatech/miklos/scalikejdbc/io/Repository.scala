package lunatech.miklos.scalikejdbc.io

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.IO
import cats.implicits.toTraverseOps
import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Order, OrderItem}
import scalikejdbc.{ConnectionPool, DB, DBSession, Tx, TxBoundary, scalikejdbcSQLInterpolationImplicitDef}

def catsEffectIOTxBoundary[A]: TxBoundary[IO[A]] = new TxBoundary[IO[A]] {

  def finishTx(result: IO[A], tx: Tx): IO[A] = {
    result.guaranteeCase {
      case _: Succeeded[IO, Throwable, A] => IO(tx.commit())
      case _: Canceled[IO, Throwable, A] => IO(tx.rollback())
      case _: Errored[IO, Throwable, A] => IO(tx.rollback())
    }
  }

  override def closeConnection(result: IO[A], doClose: () => Unit): IO[A] =
    result.guarantee(IO(doClose()))
}

class Repository(connectionPool: ConnectionPool) {

  def addOrderItems(orderId: String, items: Seq[OrderItem]): IO[Option[Order]] =
    IO.blocking {
      DB(connectionPool.borrow()).localTx { implicit session =>
        println(s"${timestamp()} Add order items $orderId in thread ${Thread.currentThread().getName}")
        items.traverse { item =>
          IO.println(s"${timestamp()} Add order item $orderId ${item.productId} in thread ${Thread.currentThread().getName}") >>
            allocateStock(orderId, item) >>
            addOrderItem(orderId, item)
        }
      }(boundary = catsEffectIOTxBoundary)
    }.flatten >> getOrder(orderId)


  private def addOrderItem(orderId: String, item: OrderItem)(implicit session: DBSession): IO[Int] =
    IO.println(s"${timestamp()} Add order item $orderId ${item.productId} in thread ${Thread.currentThread().getName}") >>
      IO(
        sql"insert into order_items (order_id, product_id, quantity) values ($orderId, ${item.productId}, ${item.quantity})"
          .update()
      )

  private def allocateStock(orderId: String, item: OrderItem)(implicit session: DBSession): IO[Int] =
    IO.println(s"${timestamp()} Allocate stock $orderId ${item.productId} in thread ${Thread.currentThread().getName}") >>
      IO(
        sql"update product_stock set available = available - ${item.quantity} where product_id = ${item.productId} and available >= ${item.quantity}"
          .update()
      ).flatMap(updated =>
        if updated == 0 then
          IO.raiseError(new IllegalStateException(s"There is not enough stock available for product ${item.productId}"))
        else
          IO(updated)
      )

  def getOrder(orderId: String): IO[Option[Order]] = {
    IO.blocking {
      DB(connectionPool.borrow()).readOnly { implicit session =>

        val items = sql"select product_id, quantity from order_items where order_id = $orderId"
          .map(rs => OrderItem(rs.string(1), rs.int(2)))
          .list
          .apply()

        Option.when(items.nonEmpty)(Order(orderId, items))
      }
    }
  }

  def getOrders: Seq[Order] = {
    DB(connectionPool.borrow()).readOnly { implicit session =>

      sql"select order_id, product_id, quantity from order_items"
        .map(rs => rs.string(1) -> OrderItem(rs.string(2), rs.int(3)))
        .list
        .apply()
        .groupBy((id, item) => id)
        .map((id, items) => Order(id, items.map(_._2)))
        .toSeq
    }
  }

}

