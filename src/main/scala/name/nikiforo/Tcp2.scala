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

class Tcp2[F[_]: Concurrent: ContextShift](
  pipe: Pipe[F, Byte, TcpResponse[Array[Byte]]],
  addr: InetSocketAddress,
) extends StreamStrategy[F, Resource[F, Socket[F]]] {

  type StreamResponse = Stream[F, TcpResponse[Array[Byte]]]

  protected val streams =
    for {
      blocker <- Stream.resource(Blocker[F])
      socketGroup <- Stream.resource(SocketGroup[F](blocker))
      (_, streams) <- Stream.resource(socketGroup.serverResource[F](addr, receiveBufferSize = 1024 * 1024))
      socket <- streams
    } yield socket

  protected def handle(clientResource: Resource[F, Socket[F]]): Stream[F, TcpResponse[Array[Byte]]] =
    for {
      socket <- Stream.resource(clientResource)
      response <- handleSocket(socket)
    } yield response

  private def handleSocket(socket: Socket[F]): StreamResponse =
    socket
      .reads(8192)
      .through(pipe)
      .flatMap {
        case OutputResponse(v) => Stream.eval_(socket.write(Chunk.bytes(v)))
        case other => Stream.emit(other)
      }
      .onComplete(Stream.eval_(socket.endOfOutput))
}
