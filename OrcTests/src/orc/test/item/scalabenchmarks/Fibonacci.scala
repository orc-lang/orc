package orc.test.item.scalabenchmarks

object Fibonacci extends BenchmarkApplication[Unit, Seq[Long]] with ExpectedBenchmarkResult[Seq[Long]] {
  def fibNaive(n: Long): Long = if (n <= 1) n else fibNaive(n - 1) + fibNaive(n - 2)

  def fibPair(n: Long): Long = {
    def fib(n: Long): (Long, Long) = {
      if (n == 0) (0, 0)
      else if (n == 1) (1, 0)
      else {
        val (f1, f2) = fib(n - 1)
        (f1 + f2, f1)
      }
    }
    fib(n)._1
  }

  def fibPairTailrec(n: Long): Long = {
    def fib(i: Long, curr: Long, prev: Long): Long = {
      if (i == n) curr
      else {
        fib(i + 1, curr + prev, curr)
      }
    }
    fib(0, 0, 1)
  }
  
  def setup() = ()
  val size = 1
  val name = "Fibonacci"
  
  def benchmark(ctx: Unit) = {
    val a = fibPairTailrec(3000)
    val b = fibPairTailrec(1000)
    val c = fibPair(1000)
    val d = fibNaive(19)
    List(a, b, c, d)
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x562e88d8,
      10 -> 0x562e88d8,
      100 -> 0x562e88d8,
      )
}
