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

object Server extends IOApp {

  def run(args : List[String]): IO[ExitCode] = {
      val action = for {
        rpcCounter <- Stream.eval(RpcRefCounter[IO])
        rpcStream = launchRpcCounter(rpcCounter)
        serverStream = server(rpcCounter)
        _ <- Stream(serverStream, rpcStream).parJoinUnbounded
      } yield {}

      action
        .compile
        .drain
        .recoverWith { ex => IO.delay(println(s"DISASTER: ${ex.getMessage}")) }
        .as(ExitCode.Success)
  }

  private def launchRpcCounter(rpcCounter: RpcRefCounter[IO]): Stream[IO, Unit] =
    Stream
      .repeatEval(rpcCounter.stats)
      .metered(1.seconds)
      .evalMap { rpcs => IO.delay(println(s"got $rpcs messages")) }

  private def server(rpcCounter: RpcRefCounter[IO]) = {
    val cr2: Pipe[IO, Messages, TcpResponse[Array[Byte]]] = new CR2

    val decoder = Message.parser3.map { case _ => MessageStub }

    val hc: Pipe[IO, Byte, TcpResponse[Array[Byte]]] =
      in =>
        in.through(StreamDecoder.many(decoder).toPipeByte)
          .flatMap(msg => fs2.Stream(OutputResponse(msg), Request))
          .throughFlatResponse(cr2)

    val server = new Tcp2(hc, new InetSocketAddress("127.0.0.1", 20111))

    server.run.evalMap {
      case Request => rpcCounter.handle
      case LogResponse(ls) => IO.delay(println(ls))
      case _ => IO.unit
    }
  }
}
