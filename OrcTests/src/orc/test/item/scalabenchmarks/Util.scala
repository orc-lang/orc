package orc.test.item.scalabenchmarks

import orc.lib.{ Benchmark, BenchmarkTimes }

object Util {
  def time[T](iteration: Int, size: Double)(op: => T): (BenchmarkTimes, T) = {
    val startTime = Benchmark.getTimes()
    val res = op
    val time = Benchmark.endBenchmark(startTime, iteration, size)

    //println("Compute of %s took %fs".format(c.expr.toString, time/1000000000.0))
    (time, res)
  }

  def timeIt[T](op: => T): T = {
    val (t, v) = time(0, 1)(op)
    println(s"Compute took $t")
    v
  }

  def thread[T](op: => T): ControllableThread[T] = {
    val t = new ControllableThread(op)
    t.start()
    t
  }
}

class ControllableThread[+T](op: => T) extends Thread {
  private[this] var result: Option[T] = null
  override def run() = {
    try {
      val v = op
      synchronized {
        result = Some(v)
      }
    } catch {
      case _: InterruptedException =>
        synchronized {
          result = None
        }
      case e: Throwable =>
        synchronized {
          result = None
        }
        throw e
    }
  }

  def terminate() = {
    interrupt()
  }

  def value(): Option[T] = {
    join()
    synchronized {
      assert(result != null)
      result
    }
  }
}
