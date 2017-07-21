package orc.scalabenchmarks

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Threads {
  import scala.concurrent.ExecutionContext.Implicits.global

  /*
	This program creates 2^20 (or about 1 million) threads
	and waits for them to terminate.
	*/

  val N = 18
  def threads(n: Int): Unit = {
    if (n != 0) {
      val t = Future {
        threads(n - 1)
      }
      threads(n - 1)
      Await.ready(t, Duration.Inf)
    }
  }

  def main(args: Array[String]): Unit = {
    // Don't actually run it. It will DOS, at least linux systems, by spawning 2^N threads. Good fun.
    //threads(N)
    //ts.foreach(_.join())
  }
}
