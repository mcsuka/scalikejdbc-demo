package lunatech.miklos.scalikejdbc.sequential

import lunatech.miklos.scalikejdbc.Database.getOrders
import lunatech.miklos.scalikejdbc.TestData.log
import lunatech.miklos.scalikejdbc.{Database, OrderItem, TestData}
import scalikejdbc.ConnectionPool

import scala.util.{Failure, Success}

object Application {

  def main(args: Array[String]): Unit = {
    val repo = Repository(Database.init())

    sequentialExecution(repo)
  }

  private def sequentialExecution(repo: Repository): Unit = {

    TestData.orders.foreach(order =>
      repo.addOrderItemsSequential(order.orderId, order.items) match {
        case Left(exception) => log(s"order ${order.orderId} failed: $exception")
        case Right(value) => log(s"order ${order.orderId} succeeded: $value")
      }
    )

    log("Pending Orders:\n" + getOrders.mkString("\n"))
  }

}
