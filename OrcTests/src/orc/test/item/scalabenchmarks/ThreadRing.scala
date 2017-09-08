package orc.test.item.scalabenchmarks

import scala.concurrent.Channel

/*
Threadring
    * create 503 linked threads (named 1 to 503)
    * thread 503 should be linked to thread 1, forming an unbroken ring
    * pass a token to thread 1
    * pass the token from thread to thread N times
    * print the name of the last thread (1 to 503) to take the token

Description from
http://shootout.alioth.debian.org/u32q/benchmark.php?test=threadring&lang=all
*/

object ThreadRing extends BenchmarkApplication {
  import Util._

  def threadRing(id: Int, m: Int, in: Channel[Int], next: Channel[Int]): Int = {
    val x = in.read
    if (m == x)
      id
    else {
      next.write(x + 1)
      threadRing(id, m, in, next)
    }
  }

  val N = 503

  def threadRingRunner(p: Int): Unit = {
    val ring = (0 until N).map(_ => new Channel[Int]()).toArray
    ring(0).write(0)

    val result = synchronized {
      var result: Option[Int] = None
      val threads = (0 until N).map(i => thread {
        result = Some(threadRing(i + 1, p, ring(i), ring((i + 1) % N)))
        synchronized {
          notify()
        }
      })

      while (result.isEmpty) {
        wait()
      }

      threads.foreach(_.terminate())

      result.get
    }

    println(result)
  }

  def main(args: Array[String]): Unit = {
    threadRingRunner(2000)
    threadRingRunner(20000)
  }
}

