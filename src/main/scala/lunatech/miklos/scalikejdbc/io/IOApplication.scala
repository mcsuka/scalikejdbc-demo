package lunatech.miklos.scalikejdbc.io

import cats.effect.kernel.Outcome

import scala.concurrent.duration.DurationInt
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.parallel.*
import lunatech.miklos.scalikejdbc.Database.getOrders
import lunatech.miklos.scalikejdbc.TestData.log
import lunatech.miklos.scalikejdbc.{Database, Order, OrderItem, TestData}
import scalikejdbc.ConnectionPool

object IOApplication extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val repo = Repository(Database.init())

    TestData.orders
      .map(order => repo.addOrderItems(order.orderId, order.items))
      .zipWithIndex
      .parTraverse((io, idx) =>
        io.start.flatMap(fiber =>
          fiber.join.map {
            case Outcome.Succeeded(fa) => log(s"Order $idx success: $fa")
            case Outcome.Errored(e) => log(s"Order $idx failure: $e")
            case Outcome.Canceled() => log(s"Order $idx cancelled")
          }
        )
      )
      .andWait(300.millis)
      .map(_ => log("Pending Orders:\n" + getOrders.mkString("\n")))
      .as(ExitCode.Success)
  }
}
