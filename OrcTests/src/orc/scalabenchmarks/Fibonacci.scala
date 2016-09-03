package orc.scalabenchmarks

object Fibonacci {
  def fibNaive(n: BigInt): BigInt = if (n <= 1) n else fibNaive(n - 1) + fibNaive(n - 2)

  def fibPair(n: BigInt): BigInt = {
    def fib(n: BigInt): (BigInt, BigInt) = {
      if (n == 0) (0, 0)
      else if (n == 1) (1, 0)
      else {
        val (f1, f2) = fib(n - 1)
        (f1 + f2, f1)
      }
    }
    fib(n)._1
  }

  def fibPairTailrec(n: BigInt): BigInt = {
    def fib(i: BigInt, curr: BigInt, prev: BigInt): BigInt = {
      if (i == n) curr
      else {
        fib(i + 1, curr + prev, curr)
      }
    }
    fib(0, 0, 1)
  }
  def main(args: Array[String]) {
    Util.timeIt {
      println(fibPairTailrec(10000))
      println(fibPairTailrec(1000))
      println(fibPair(1000))
      println(fibNaive(20))
    }
  }
}