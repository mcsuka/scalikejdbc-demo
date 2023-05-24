package lunatech.miklos.scalikejdbc.future

import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Database, OrderItem, TestData}
import scalikejdbc.ConnectionPool

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

import scala.util.{Failure, Success}

object Application {

  def main(args: Array[String]): Unit = {
    val repo = Repository(Database.init())

    parallelExecution(repo)
  }

  private def parallelExecution(repo: Repository): Unit = {
    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8)) // change to 4

    TestData.orders.foreach((id, items) =>
      repo
//        .addOrderItems(id, items)
        .addOrderItemsParallel(id, items)
        .onComplete {
          case Success(value) => println(s"${timestamp()} order $id succeeded: $value")
          case Failure(exception) => println(s"${timestamp()} order $id failed: $exception")
        }
    )

    Thread.sleep(1000)
    println("\nPending Orders:\n" + repo.getOrders.mkString("\n"))
  }

}
