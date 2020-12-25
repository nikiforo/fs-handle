package name.nikiforo

import cats.effect.IOApp
import scodec.bits._
import cats.effect.IO
import fs2.Stream
import cats.effect.ExitCode
import cats.effect.Blocker
import fs2.io.tcp.SocketGroup
import cats.effect.Concurrent
import cats.effect.ContextShift
import cats.effect.Timer
import java.net.InetSocketAddress
import scala.concurrent.duration._
import cats.effect.Sync
import scala.io.Source


object Client extends IOApp {

  private val messages = ByteVector.fromHex(getMsg("nulled.hex")).get.toArray

  def run(args : List[String]): IO[ExitCode] = {
     val action = for {
        blocker <- Stream.resource(Blocker[IO])
        socketGroup <- Stream.resource(SocketGroup[IO](blocker))
        _ <- Stream(muteErrors(client[IO](socketGroup))).repeat.parJoin(1000)
     } yield {}

     action
      .compile
      .drain
      .as(ExitCode.Success)
  }

  private def client[F[_]: Concurrent: ContextShift: Timer](socketGroup: SocketGroup): Stream[F, Unit] =
    for {
      socket <- Stream.resource(socketGroup.client[F](new InetSocketAddress("127.0.0.1", 20111)))
      _ <-
        Stream.emits(messages)
          .covary[F]
          .metered(1.millis)
          .through(socket.writes())
          .drain
    } yield {}

  private def muteErrors[F[_]: Sync, O](stream: Stream[F, O]) = stream.handleErrorWith(ex => Stream.empty)

  private def getMsg(filename: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/$filename"))
    val lines = source.getLines().toList
    source.close()
    lines match {
      case List(line) => line
      case _ => ???
    }
  }
}
