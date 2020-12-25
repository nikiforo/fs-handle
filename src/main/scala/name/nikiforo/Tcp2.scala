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
) {

  type StreamResponse = Stream[F, TcpResponse[Array[Byte]]]

  def run: StreamResponse =
    for {
      blocker <- Stream.resource(Blocker[F])
      socketGroup <- Stream.resource(SocketGroup[F](blocker))
      (_, streams) <- Stream.resource(socketGroup.serverResource[F](addr, receiveBufferSize = 1024 * 1024))
      acquiredLog = s"acquired ${addr.getHostName} ${addr.getPort}".i
      response <- Stream.emit(acquiredLog) ++ streams.map(handleClient).parJoinUnbounded
    } yield response

  private def handleClient(clientResource: Resource[F, Socket[F]]): StreamResponse = {
    val stream =
      for {
        socket <- Stream.resource(clientResource)
        addr <- Stream.eval(socket.remoteAddress)
        logId <- Stream.eval(Sync[F].delay(addr.toString + Random.nextInt()))
      } yield (socket, logId)

    val resultingStream = for {
      (socket, _) <- stream
      response <-
        handleNoLogging(socket)
          .handleErrorWith {
            ex => Stream.emit(s"exception handling socket ${ex.getMessage}".i)
          }
    } yield response

    resultingStream.handleErrorWith(ex => Stream.emit(s"exception opening socket ${ex.getMessage}".i))
  }

  private def handleNoLogging(socket: Socket[F]): StreamResponse =
    socket
      .reads(8192)
      .through(pipe)
      .flatMap {
        case OutputResponse(v) => Stream.eval_(socket.write(Chunk.bytes(v)))
        case other => Stream.emit(other)
      }
      .onComplete(Stream.eval_(socket.endOfOutput))
}
