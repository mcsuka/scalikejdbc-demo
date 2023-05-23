package lunatech.miklos.scalikejdbc.vanilla

import lunatech.miklos.scalikejdbc.{Order, OrderItem}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scalikejdbc.{ConnectionPool, DB, DBSession, SQL, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import scala.collection.immutable.{AbstractSeq, LinearSeq}

class Repository()(implicit executionContext: ExecutionContext, connectionPool: ConnectionPool) {

  def addOrderItems(orderId: String, items: Seq[OrderItem]): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      Future
        .sequence(
          items.map { item =>
            allocateStock(item).flatMap(_ => addOrderItem(orderId, item))
          }
        )
    }.flatMap(_ => getOrder(orderId))


  private def addOrderItemsRecursive(orderId: String, items: Seq[OrderItem])
    (implicit session: DBSession): Future[Unit] =
    items match {
      case Nil =>
        Future.successful(())
      case head :: Nil =>
        allocateStock(head)
          .flatMap(_ => addOrderItem(orderId, head))
          .map(_ => ())
      case head :: tail =>
        allocateStock(head)
          .flatMap(_ => addOrderItem(orderId, head))
          .flatMap(_ => addOrderItemsRecursive(orderId, tail))
    }


  def addOrderItems2(orderId: String, items: Seq[OrderItem]): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      addOrderItemsRecursive(orderId, items)
    }.flatMap(_ => getOrder(orderId))

  private def addOrderItem(orderId: String, item: OrderItem)(implicit session: DBSession): Future[Int] =
    Future {
      blocking {
        sql"insert into order_items (order_id, product_id, quantity) values ($orderId, ${item.productId}, ${item.quantity})"
          .update()
      }
    }

  private def allocateStock(item: OrderItem)(implicit session: DBSession): Future[Int] =
    Future {
      blocking {
        val updated = sql"update product_stock set available = available - ${item.quantity} where product_id = ${item.productId} and available >= ${item.quantity}"
          .update()

        if (updated == 0)
          throw new IllegalStateException(s"There is not enough stock available for product ${item.productId}")

        updated
      }
    }

  def getOrder(orderId: String): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      Future {
        blocking {
          val items = sql"select product_id, quantity from order_items where order_id = $orderId"
            .map(rs => OrderItem(rs.string(1), rs.int(2)))
            .list
            .apply()

          Option.when(items.nonEmpty)(Order(orderId, items))
        }
      }
    }

  def getOrders: Seq[Order] =
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
