package lunatech.miklos.scalikejdbc.sequential

import lunatech.miklos.scalikejdbc.TestData.log
import lunatech.miklos.scalikejdbc.{Order, OrderItem}
import scalikejdbc.{ConnectionPool, DB, DBSession, SQL, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.util.{Failure, Success, Try}

class Repository(connectionPool: ConnectionPool) {

  def addOrderItemsSequential(orderId: String, items: Seq[OrderItem]): Either[Throwable, Order] = {
    Try {
      DB(connectionPool.borrow()).localTx { implicit session =>
        log(s"Add order items $orderId")
        items.foreach { item =>
          log(s"Add order item $orderId ${item.productId}")

          val allocated =
            sql"""
              update product_stock set available = available - ${item.quantity}
              where product_id = ${item.productId} and available >= ${item.quantity}"""
              .update()

          if (allocated > 0) {
            sql"""
              insert into order_items (order_id, product_id, quantity)
              values ($orderId, ${item.productId}, ${item.quantity})"""
              .update()
          } else {
            throw IllegalStateException(s"There is not enough stock available for product ${item.productId}")
          }
        }
      }
    } match {
      case Failure(exception) =>
        Left(exception)
      case Success(value) =>
        getOrder(orderId).toRight(IllegalStateException(s"Could not retrieve order with id $orderId"))
    }

  }

  def getOrder(orderId: String): Option[Order] = {
    log(s"Get order $orderId")
    Try {
      DB(connectionPool.borrow()).readOnly { implicit session =>
        val items = sql"select product_id, quantity from order_items where order_id = $orderId"
          .map(rs => OrderItem(rs.string(1), rs.int(2)))
          .list
          .apply()

        Option.when(items.nonEmpty)(Order(orderId, items))
      }
    }.getOrElse(None)
  }
}
