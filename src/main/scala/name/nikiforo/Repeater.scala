package name.nikiforo

import java.net.InetSocketAddress

import cats.effect.Blocker
import cats.effect.Concurrent
import cats.effect.ContextShift
import cats.effect.Resource
import cats.effect.Sync
import fs2.Chunk
import fs2.Pipe
import fs2.Stream
import fs2.io.tcp.Socket
import fs2.io.tcp.SocketGroup
import TcpResponse._

import scala.util.Random
import scodec.bits.ByteVector

class Repeater[F[_]](
  pipe: Pipe[F, Byte, TcpResponse[Array[Byte]]],
  hex: ByteVector,
) extends StreamStrategy[F, Array[Byte]]{

  protected val streams = Stream(hex.toArray).repeat

  protected def handle(ins: Array[Byte]): Stream[F, TcpResponse[Array[Byte]]] =
    Stream
      .emits(ins)
      .through(pipe)
      .flatMap {
        case OutputResponse(v) => Stream.empty
        case other => Stream.emit(other)
      }
}