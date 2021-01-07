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

  def outputResponse[V](v: V): TcpResponse[V] = OutputResponse(v)

  case class LogResponse(logs: String) extends TcpResponse[Nothing]

  implicit class logRecordOpt2(val lr: String) extends AnyVal {
    def i: LogResponse = LogResponse(lr)
  }
}
