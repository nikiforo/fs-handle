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

  private val parser =
    constant(hex"2424") ~
      Common.extensible1byte.flatZip { case(_, tpe) =>
        Common.ignore16 ~
          conditional(tpe != 2, Common.extensible2bytes.flatZip(a1 => ignore((a1._2+2)*8) ))
      }

  def run(args : List[String]): IO[ExitCode] =
    server
      .compile
      .drain
      .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
      .as(ExitCode.Success)

  private def server = {
    val pipe: Pipe[IO, Unit, TcpResponse[Array[Byte]]] = _.map(_ => OutputResponse("this_is_stub".getBytes()))
    val decoder = parser.map { case _ => {}}

    val hc: Pipe[IO, Byte, TcpResponse[Array[Byte]]] =
      in =>
        in.through(StreamDecoder.many(decoder).toPipeByte)
          .flatMap(msg => fs2.Stream(OutputResponse(msg), Request))
          .throughFlatResponse(pipe)

    val server = new Repeater(hc, hex"2424000000000e000000000000000000000000000000002421")

    server.run.evalMap {
      case Request => IO.unit
      case LogResponse(ls) => IO.delay(println(ls))
      case _ => IO.unit
    }
  }
}
