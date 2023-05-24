package lunatech.miklos.scalikejdbc.io

import cats.effect.kernel.Outcome

import scala.concurrent.duration.DurationInt
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.parallel.*
import lunatech.miklos.scalikejdbc.TestData.timestamp
import lunatech.miklos.scalikejdbc.{Database, Order, OrderItem, TestData}
import scalikejdbc.ConnectionPool

object IOApplication extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val repo = Repository(Database.init())

    TestData.orders
      .map((id, items) => repo.addOrderItems(id, items))
      .zipWithIndex
      .parTraverse((io, idx) =>
        io.start.flatMap(fiber =>
          fiber.join.flatMap {
            case Outcome.Succeeded(fa) => IO.println(s"${timestamp()} Order $idx success: $fa")
            case Outcome.Errored(e) => IO.println(s"${timestamp()} Order $idx failure: $e")
            case Outcome.Canceled() => IO.println(s"${timestamp()} Order $idx cancelled")
          }
        )
      )
      .andWait(300.millis)
      .flatMap(_ => IO.println("\nPending Orders:\n" + repo.getOrders.mkString("\n")))
      .as(ExitCode.Success)
  }
}
