package lunatech.miklos.scalikejdbc.future

import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Order, OrderItem}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scalikejdbc.{ConnectionPool, DB, DBSession, SQL, WrappedResultSet, scalikejdbcSQLInterpolationImplicitDef}

import scala.collection.immutable.{AbstractSeq, LinearSeq}

class Repository(connectionPool: ConnectionPool) {

  def addOrderItems(orderId: String, items: Seq[OrderItem])
    (implicit executionContext: ExecutionContext): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      println(s"${timestamp()} Add order items $orderId in thread ${Thread.currentThread().getName}")
      Future {
        blocking {
          items.foreach { item =>
            println(s"${timestamp()} Add order item $orderId ${item.productId} in thread ${Thread.currentThread().getName}")

            val allocated =
              sql"""
              update product_stock set available = available - ${item.quantity}
              where product_id = ${item.productId} and available >= ${item.quantity}"""
                .update()

            if (allocated == 0)
              throw new IllegalStateException(s"There is not enough stock available for product ${item.productId}")

            sql"""
            insert into order_items (order_id, product_id, quantity)
            values ($orderId, ${item.productId}, ${item.quantity})"""
              .update()
          }
        }
      }
    }.map(_ => getOrder(orderId))

  def addOrderItemsParallel(orderId: String, items: Seq[OrderItem])
    (implicit executionContext: ExecutionContext): Future[Option[Order]] =
    DB(connectionPool.borrow()).futureLocalTx { implicit session =>
      println(s"${timestamp()} Add order items $orderId in thread ${Thread.currentThread().getName}")
      addOrderItemsRecursive(orderId, items)
    }.map(_ => getOrder(orderId))

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
        println(s"${timestamp()} Add order item $orderId ${item.productId} in thread ${Thread.currentThread().getName}")
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
        println(s"${timestamp()} Allocate stock $orderId ${item.productId} in thread ${Thread.currentThread().getName}")
        val updated =
          sql"""
          update product_stock set available = available - ${item.quantity}
          where product_id = ${item.productId} and available >= ${item.quantity}"""
            .update()

        if (updated == 0)
          throw new IllegalStateException(s"There is not enough stock available for product ${item.productId}")
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
