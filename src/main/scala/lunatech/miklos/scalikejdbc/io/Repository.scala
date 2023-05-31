package lunatech.miklos.scalikejdbc.io

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.IO
import cats.implicits.toTraverseOps
import lunatech.miklos.scalikejdbc.TestData.log
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
        log(s"Add order items $orderId")
        items.traverse { item =>
          log(s"Add order item $orderId ${item.productId}")
          allocateStock(orderId, item) >> addOrderItem(orderId, item)
        }
      }(boundary = catsEffectIOTxBoundary)
    }.flatten >> getOrderIO(orderId)


  private def addOrderItem(orderId: String, item: OrderItem)(implicit session: DBSession): IO[Int] =
    IO {
      log(s"Add order item $orderId ${item.productId}")
      sql"insert into order_items (order_id, product_id, quantity) values ($orderId, ${item.productId}, ${item.quantity})"
        .update()
    }

  private def allocateStock(orderId: String, item: OrderItem)(implicit session: DBSession): IO[Int] = {
    log(s"Allocate stock $orderId ${item.productId}")
    IO(
      sql"update product_stock set available = available - ${item.quantity} where product_id = ${item.productId} and available >= ${item.quantity}"
        .update()
    ).flatMap(updated =>
      if updated == 0 then
        IO.raiseError(new IllegalStateException(s"There is not enough stock available for product ${item.productId}"))
      else
        IO(updated)
    )
  }

  private def getOrderIO(orderId: String): IO[Option[Order]] = {
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
}

