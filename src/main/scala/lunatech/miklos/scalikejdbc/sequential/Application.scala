package lunatech.miklos.scalikejdbc.sequential

import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Database, OrderItem, TestData}
import scalikejdbc.ConnectionPool

import scala.util.{Failure, Success}

object Application {

  def main(args: Array[String]): Unit = {
    val repo = Repository(Database.init())

    sequentialExecution(repo)
  }

  private def sequentialExecution(repo: Repository): Unit = {

    TestData.orders.foreach((id, items) =>
      repo.addOrderItemsSequential(id, items) match {
        case Failure(exception) => println(s"${timestamp()} order $id failed: $exception")
        case Success(value) => println(s"${timestamp()} order $id succeeded: $value")
      }
    )

    println("\nPending Orders:\n" + repo.getOrders.mkString("\n"))
  }

}
