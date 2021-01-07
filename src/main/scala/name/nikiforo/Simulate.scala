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
      .evalMap {
        case Some(log) => IO.delay(println(log))
        case None => IO.unit
      }
      .compile
      .drain
      .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
      .as(ExitCode.Success)

  private def clientPipe(ins: Stream[IO, Byte]): Stream[IO, Option[String]] = {
    val stream =
      for {
        in <- ins
        v <-
          in match {
            case H42 => Stream({})
            case H24 => Stream.raiseError[IO](new IllegalArgumentException("not 42!"))
          }
      } yield {}
    
    for {
      broadList <- stream.through(Broadcast(2)).pull.take(2).void.streamNoScope.foldMap(List(_))
      first = broadList.head
      second = broadList(1)
      response <- first.merge(second)
    } yield none
  }

  private def logError(ex: Throwable) = Stream.emit(s"exception handling stream ${ex.getMessage}".some)
}
