package name.nikiforo

import cats.Monad
import cats.effect.Clock
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._

final class RpcRefCounter[F[_]: Clock: Monad] private(ref: Ref[F, Long]) {

  val handle: F[Unit] = ref.update(_ + 1)

  val stats: F[Long] = ref.getAndSet(0)
}

object RpcRefCounter {

  def apply[F[_]: Sync: Clock]: F[RpcRefCounter[F]] =
    Ref[F].of(0L).map(ref => new RpcRefCounter(ref))
}
