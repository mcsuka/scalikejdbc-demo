package lunatech.miklos.scalikejdbc.vanilla

import lunatech.miklos.scalikejdbc.{Database, OrderItem}
import scalikejdbc.ConnectionPool

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Application {

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

    implicit val connectionPool: ConnectionPool = Database.init()
    val repo = Repository()

    val order1 = repo.addOrderItems2("o001", Seq(OrderItem("apple", 4), OrderItem("pear", 5)))
    val order2 = repo.addOrderItems2("o002", Seq(OrderItem("apple", 4), OrderItem("pear", 6)))

    order1.onComplete {
      case Success(value) => println(s"order1 succeeded: $value")
      case Failure(exception) => println(s"order1 failed: $exception")
    }

    order2.onComplete {
      case Success(value) => println(s"order2 succeeded: $value")
      case Failure(exception) => println(s"order2 failed: $exception")
    }

    Thread.sleep(300)
    println("\nPending Orders:\n" + repo.getOrders.mkString("\n"))
    System.exit(0)
  }

}
