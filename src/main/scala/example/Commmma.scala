package example

import cats.{Comonad, Functor, Monad}
import cats.implicits._

final case class MyStore[S, A](here: S, view: S => A) {
  def move(s: S): MyStore[S, A] = {
    val x: MyStore[S, MyStore[S, A]] = this.coflatten
    println(x)
    x.view(s)
  }
}
object MyStore {
  type ComonadF[S] = Comonad[({ type L[A] = MyStore[S, A] })#L]
  implicit def comonadStore[S]: ComonadF[S] = new ComonadF[S] {
    override def extract[A](x: MyStore[S, A]): A = x.view(x.here)
    override def coflatMap[A, B](fa: MyStore[S, A])(
        f: MyStore[S, A] => B
    ): MyStore[S, B] =
      fa.copy(view = (next: S) => f(MyStore(next, fa.view)))
    override def map[A, B](fa: MyStore[S, A])(
        f: A => B
    ): MyStore[S, B] = fa.copy(view = fa.view.andThen(f))
  }
}

trait Co[W[_], A] {
  def run[R]: W[A => R] => R
}
object Co {
  def monadCo[W[_]: Comonad]: Monad[({ type L[A] = Co[W, A] })#L] =
    new Monad[
      ({
        type L[A] = Co[W, A]
      })#L
    ] {
      override def flatMap[A, B](k: Co[W, A])(
          f: A => Co[W, B]
      ): Co[W, B] = new Co[W, B] {
        override def run[R]: W[B => R] => R = {
          val z: W[B => R] => (A => R) =
            ((wa: W[B => R]) => (a: A) => (f(a).run)(wa))
          ((wbr: W[B => R]) => wbr.coflatMap(z)).andThen(k.run)
        }
      }
      override def tailRecM[A, B](a: A)(
          f: A => Co[W, Either[A, B]]
      ): Co[W, B] = ???
      override def pure[A](x: A): Co[W, A] = new Co[W, A] {
        override def run[R]: W[A => R] => R =
          _.extract(x)
      }
    }

  def select[W[_]: Comonad, A, B](co: Co[W, A => B])(w: W[A]): W[B] = {
    def dist(fs: W[A])(f: A => B): W[B] = fs.map(f)
    val x: W[(A => B) => W[B]] = w.coflatMap(dist)
    val ff: W[(A => B) => W[B]] => W[B] = (co.run)
    ff(x)
  }
}

object StoreMain extends App {
  val s = MyStore(1, identity[Int])
  println(s)
  println(s.move(2))
}
