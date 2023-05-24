package lunatech.miklos.scalikejdbc.sequential

import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Order, OrderItem}
import scalikejdbc.{ConnectionPool, DB, DBSession, SQL, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.util.Try

class Repository(connectionPool: ConnectionPool) {

  def addOrderItemsSequential(orderId: String, items: Seq[OrderItem]): Try[Order] = {
    Try {
      DB(connectionPool.borrow()).localTx { implicit session =>
        println(s"${timestamp()} Add order items $orderId in thread ${Thread.currentThread().getName}")
        items.foreach { item =>
          println(s"${timestamp()} Add order item $orderId ${item.productId} in thread ${Thread.currentThread().getName}")

          val allocated =
            sql"""
              update product_stock set available = available - ${item.quantity}
              where product_id = ${item.productId} and available >= ${item.quantity}"""
              .update()

          if (allocated == 0) {
            throw new IllegalStateException(s"There is not enough stock available for product ${item.productId}")
          }

          sql"""
          insert into order_items (order_id, product_id, quantity)
          values ($orderId, ${item.productId}, ${item.quantity})"""
            .update()
        }
      }
      getOrder(orderId).getOrElse(throw new IllegalStateException(s"Could not retrieve order with id $orderId"))
    }
  }

  def getOrder(orderId: String): Option[Order] = {
    println(s"${timestamp()} Get order $orderId in thread ${Thread.currentThread().getName}")
    DB(connectionPool.borrow()).readOnly { implicit session =>
      val items = sql"select product_id, quantity from order_items where order_id = $orderId"
        .map(rs => OrderItem(rs.string(1), rs.int(2)))
        .list
        .apply()

      Option.when(items.nonEmpty)(Order(orderId, items))
    }
  }

  def getOrders: Iterable[Order] =
    DB(connectionPool.borrow()).readOnly { implicit session =>
      sql"select order_id, product_id, quantity from order_items"
        .map(rs => rs.string(1) -> OrderItem(rs.string(2), rs.int(3)))
        .list
        .apply()
        .groupBy((id, item) => id)
        .map((id, items) => Order(id, items.map(_._2)))
    }

}
