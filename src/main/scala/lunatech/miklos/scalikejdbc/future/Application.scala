package lunatech.miklos.scalikejdbc.future

import lunatech.miklos.scalikejdbc.Database.getOrders
import lunatech.miklos.scalikejdbc.TestData.log
import lunatech.miklos.scalikejdbc.{Database, OrderItem, TestData}
import scalikejdbc.ConnectionPool

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Application {

  def main(args: Array[String]): Unit = {
    val repo = Repository(Database.init())

    log("Starting Application")

    parallelExecution(repo)
  }

  private def parallelExecution(repo: Repository): Unit = {
    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8)) // change to 4

    TestData.orders.foreach(order =>
      repo
        .addOrderItems(order.orderId, order.items)
        .onComplete {
          case Success(value) => log(s"order ${order.orderId} succeeded: $value")
          case Failure(exception) => log(s"order ${order.orderId} failed: $exception")
        }
    )

    Thread.sleep(300)
    log("Pending Orders:\n" + getOrders.mkString("\n"))
  }

}
