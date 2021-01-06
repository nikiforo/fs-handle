package name.nikiforo

import fs2.Stream
import TcpResponse._
import cats.effect.Concurrent

abstract class StreamStrategy[F[_], O] {

  final def run: Stream[F, TcpResponse[Array[Byte]]] =
    Stream.emit(s"started stream".i) ++ streams.flatMap(in => handle(in).handleErrorWith(logError))

  protected def streams: Stream[F, O]

  protected def handle(ins: O): Stream[F, TcpResponse[Array[Byte]]]

  private def logError(ex: Throwable) = Stream.emit(s"exception handling stream ${ex.getMessage}".i)
}
