package lunatech.miklos.scalikejdbc.io

import scala.concurrent.duration.DurationInt
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.parallel._
import lunatech.miklos.scalikejdbc.{Database, OrderItem}
import scalikejdbc.ConnectionPool

object IOApplication extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    println("Hello CatsEffect!")

    implicit val connectionPool: ConnectionPool = Database.init()
    val repo = Repository()

    val order1 = repo.addOrderItems("o001", Seq(OrderItem("apple", 4), OrderItem("pear", 5)))
    val order2 = repo.addOrderItems("o002", Seq(OrderItem("apple", 4), OrderItem("pear", 6)))

    Seq(order1, order2)
      .parTraverse { order =>
        order.start.flatMap(_.join.flatMap(x => IO.println(s".... $x")))
      }
      .andWait(300.millis)
      .flatMap(_ => IO.println("\nPending Orders:\n" + repo.getOrders.mkString("\n")))
      .as(ExitCode.Success)

//    IO.sleep(1300.millis)
//    println("\nPending Orders:\n" + repo.getOrders.mkString("\n"))
//
//    IO(ExitCode.Success)
  }
}
