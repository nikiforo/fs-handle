package name.nikiforo

import cats.effect.Concurrent
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Broadcast
import cats.syntax.either._

sealed trait TcpResponse[+V]

object TcpResponse {

  type StreamResponse[F[_], V] = Stream[F, TcpResponse[V]]

  case class OutputResponse[V](v: V) extends TcpResponse[V]

  case class LogResponse(logs: String) extends TcpResponse[Nothing]

  case object Request extends TcpResponse[Nothing]

  implicit class logRecordOpt2(val lr: String) extends AnyVal {
    def i: LogResponse = LogResponse(lr)
  }

  implicit class partitionEitherOps[F[_], O](val stream: Stream[F, O]) {

    def partitionEither[L, R](f: O => Either[L,R])(implicit c: Concurrent[F]): Stream[F, (Stream[F, L], Stream[F, R])] =
      for {
        broadIn <- stream.map(f).through(Broadcast(2)).pull.take(2).void.streamNoScope.foldMap(List(_))
        in1 = broadIn.head
        in2 = broadIn(1)
        ls = in1.flatMap(_.fold(Stream.emit, _ => Stream.empty))
        rs = in2.flatMap(_.fold(_ => Stream.empty, Stream.emit))
      } yield (ls, rs)
  }

  implicit class streamResponseOps[F[_], O](val stream: StreamResponse[F, O]) {

    def mapResponse[O2](f: O => O2): StreamResponse[F, O2] =
      stream.map {
        case OutputResponse(o) => OutputResponse(f(o))
        case logs: LogResponse => logs
        case Request => Request
      }

    def throughFlatResponse[O2](pipe: Pipe[F, O, TcpResponse[O2]])(implicit c: Concurrent[F]): StreamResponse[F, O2] =
      for {
        broad <-
          stream.partitionEither {
            case OutputResponse(v) => v.asRight
            case log: LogResponse => log.asLeft
            case Request => Request.asLeft
          }
        (responses, outputs) = broad
        response <- outputs.through(pipe).merge(responses)
      } yield response
  }
}
