package name.nikiforo

import java.net.InetSocketAddress

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.applicativeError._
import cats.syntax.option._
import fs2.Stream
import fs2.concurrent.Broadcast

object Simulate extends IOApp {

  private val H42: Byte = 42

  private val H24: Byte = 24

  def run(args : List[String]): IO[ExitCode] =
    Stream(Stream(H42, H24))
      .repeat
      .flatMap(_.through(clientPipe).handleErrorWith(logError))
      .evalMap(_ => IO.unit)
      .compile
      .drain
      .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
      .as(ExitCode.Success)

  private def clientPipe(ins: Stream[IO, Byte]): Stream[IO, Unit] = {
    val stream =
      ins.flatMap {
        case H42 => Stream({})
        case H24 => Stream.raiseError[IO](new IllegalArgumentException("not 42!"))
      }

    for {
      broadList <- stream.through(Broadcast(1)).pull.take(1).void.streamNoScope.foldMap(List(_))
      response <- broadList.head
    } yield {}
  }

  private def logError(ex: Throwable) = Stream.eval_(IO.delay(println(s"exception handling stream ${ex.getMessage}")))
}
