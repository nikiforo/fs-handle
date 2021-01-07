package name.nikiforo

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import fs2.Stream
import fs2.Pipe

import scala.concurrent.duration._
import cats.syntax.applicativeError._
import TcpResponse._
import java.net.InetSocketAddress
import cats.syntax.either._
import fs2.concurrent.Broadcast

object Simulate extends IOApp {

  def run(args : List[String]): IO[ExitCode] =
    Stream(Stream(H42, H24))
      .repeat
      .flatMap(_.through(clientPipe).handleErrorWith(logError))
      .evalMap {
        case LogResponse(ls) => IO.delay(println(ls))
        case _ => IO.unit
      }
      .compile
      .drain
      .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
      .as(ExitCode.Success)

  val H42: Byte = 42
  val H24: Byte = 24

  private def clientPipe(ins: Stream[IO, Byte]): Stream[IO, TcpResponse[Unit]] = {
    val stream =
      for {
        in <- ins
        v <-
          in match {
            case H42 => Stream({})
            case H24 => Stream.raiseError[IO](new IllegalArgumentException("not 42!"))
          }
      } yield OutputResponse(v)
    
    for {
      broadList <- stream.through(Broadcast(2)).pull.take(2).void.streamNoScope.foldMap(List(_))
      first = broadList.head
      second = broadList(1)
      response <- first.merge(second)
    } yield response
  }

  private def logError(ex: Throwable) = Stream.emit(s"exception handling stream ${ex.getMessage}".i)
}
