package lunatech.miklos.scalikejdbc.future

import lunatech.miklos.scalikejdbc.TestData.log
import lunatech.miklos.scalikejdbc.{Order, OrderItem}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scalikejdbc.{ConnectionPool, DB, DBSession, SQL, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import scala.collection.immutable.{AbstractSeq, LinearSeq}

class Repository(connectionPool: ConnectionPool) {

  def addOrderItems(orderId: String, items: Seq[OrderItem])
    (implicit executionContext: ExecutionContext): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      log(s"Add order items $orderId")
      addOrderItemsRecursive(orderId, items)
    }.flatMap(_ => getOrder(orderId))

  private def addOrderItemsRecursive(orderId: String, items: Seq[OrderItem])
    (implicit session: DBSession, executionContext: ExecutionContext): Future[Unit] = {
    items match {
      case Nil =>
        Future.successful(())
      case head :: Nil =>
        allocateStock(orderId, head)
          .flatMap(_ => addOrderItem(orderId, head))
      case head :: tail =>
        allocateStock(orderId, head)
          .flatMap(_ => addOrderItem(orderId, head))
          .flatMap(_ => addOrderItemsRecursive(orderId, tail))
    }
  }

  private def addOrderItem(orderId: String, item: OrderItem)
    (implicit session: DBSession, executionContext: ExecutionContext): Future[Unit] =
    Future {
      blocking {
        log(s"Add order item $orderId ${item.productId}")
        sql"""
        insert into order_items (order_id, product_id, quantity)
        values ($orderId, ${item.productId}, ${item.quantity})"""
          .update()
      }
    }

  private def allocateStock(orderId: String, item: OrderItem)
    (implicit session: DBSession, executionContext: ExecutionContext): Future[Unit] =
    Future {
      blocking {
        log(s"Allocate stock $orderId ${item.productId}")

        val updated =
          sql"""
          update product_stock set available = available - ${item.quantity}
          where product_id = ${item.productId} and available >= ${item.quantity}"""
            .update()

        if (updated == 0)
          throw IllegalStateException(s"There is not enough stock available for product ${item.productId}")
      }
    }

  private def getOrder(orderId: String)
    (implicit executionContext: ExecutionContext): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      Future {
        blocking {
          log(s"Get order $orderId")

          val items = sql"select product_id, quantity from order_items where order_id = $orderId"
            .map(rs => OrderItem(rs.string(1), rs.int(2)))
            .list
            .apply()

          Option.when(items.nonEmpty)(Order(orderId, items))
        }
      }
    }
}
