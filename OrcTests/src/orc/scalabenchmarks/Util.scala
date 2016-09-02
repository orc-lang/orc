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
}
