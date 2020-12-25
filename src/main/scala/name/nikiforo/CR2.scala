package name.nikiforo

import cats.Applicative
import cats.effect.Concurrent
import cats.effect.Timer
import fs2.Pipe
import fs2.Pull
import fs2.Stream
import name.nikiforo.TcpResponse._

final class CR2[F[_]: Timer: Applicative: Concurrent, In]() extends Pipe[F, In, TcpResponse[Array[Byte]]] {

  def apply(ins: Stream[F, In]): Stream[F, TcpResponse[Array[Byte]]] =
    handlePull(ins.pull.uncons1).stream

  private def handlePull(
    pull: Pull[F, TcpResponse[Array[Byte]], Option[(In, Stream[F, In])]],
  ): Pull[F, TcpResponse[Array[Byte]], Unit] =
    pull
      .flatMap {
        case None => Pull.done
        case Some((_, stream)) =>
          stream.map(_ => OutputResponse("this_is_stub".getBytes())).pull.echo
      }
}
