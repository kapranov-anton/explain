package example

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.blocking
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._

object Main extends App {
  def load: Future[Unit] = Future {
    blocking(Thread.sleep(2000))
    println("done")
  }

  val t = new java.util.Timer
  def withTimeout[A](f: Future[A], timeout: FiniteDuration): Future[A] = {
    val p = Promise[A]().completeWith(f)
    val task = new java.util.TimerTask {
      def run() = {
        println("timer")
        p.tryFailure(new TimeoutException)
      }
    }
    t.schedule(task, timeout.toMillis)
    p.future
  }
  Await.ready(withTimeout[Unit](load, 1.seconds), Duration.Inf)
  println(111)
  t.cancel()
}
