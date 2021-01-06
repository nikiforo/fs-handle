package name.nikiforo

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import fs2.Stream
import fs2.Pipe

import scala.concurrent.duration._
import cats.syntax.applicativeError._
import scodec.stream.StreamDecoder
import TcpResponse._
import java.net.InetSocketAddress

import scodec.codecs._
import scodec.bits._

object Server extends IOApp {

  def run(args : List[String]): IO[ExitCode] =
    server
      .compile
      .drain
      .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
      .as(ExitCode.Success)

  val H42 = hex"42".toByte()
  val H24 = hex"24".toByte()

  private def server = {
    val pipe: Pipe[IO, Unit, TcpResponse[Array[Byte]]] = _.map(_ => OutputResponse("this_is_stub".getBytes()))
    val decoderPipe: Pipe[IO, Byte, Unit] =
      _.flatMap {
        case H42 => Stream({})
        case H24 => Stream.raiseError[IO](new IllegalArgumentException("not 42!"))
      }

    val hc: Pipe[IO, Byte, TcpResponse[Array[Byte]]] =
      in =>
        in.through(decoderPipe)
          .flatMap(msg => fs2.Stream(OutputResponse(msg), Request))
          .throughFlatResponse(pipe)

    val server = new Repeater(hc, ByteVector(Array(H42, H24)))

    server.run.evalMap {
      case Request => IO.unit
      case LogResponse(ls) => IO.delay(println(ls))
      case _ => IO.unit
    }
  }
}
