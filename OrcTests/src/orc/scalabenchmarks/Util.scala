package orc.scalabenchmarks

object Util {
  def time[T](op: => T): (Double, T) = {
    val startTime = System.nanoTime()
    val res = op
    val time = System.nanoTime() - startTime

    //println("Compute of %s took %fs".format(c.expr.toString, time/1000000000.0))
    (time / 1000000000.0, res)
  }

  def timeIt[T](op: => T): T = {
    val (t, v) = time(op)
    println("Compute took %fs".format(t))
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
    assert(result != null)
    result
  }
}
