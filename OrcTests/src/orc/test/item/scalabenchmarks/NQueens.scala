package orc.test.item.scalabenchmarks

import NQueensTypes.{ Queen, Solutions }

object NQueensTypes {
  type Queen = (Int, Int)
  type Solutions = Iterable[List[Queen]]
}

// From: https://gist.github.com/ornicar/1115259

// Solves the n-queens problem for an arbitrary board size
// Run for a board size of ten: scala nqueen.scala 10
object NQueens extends BenchmarkApplication[Unit, Solutions] with ExpectedBenchmarkResult[Solutions] {
  val N = 8 + BenchmarkConfig.problemSize

  def isSafe(queen: Queen, others: List[Queen]) =
    others forall (!isAttacked(queen, _))

  def isAttacked(q1: Queen, q2: Queen) =
    q1._1 == q2._1 ||
      q1._2 == q2._2 ||
      (q2._1 - q1._1).abs == (q2._2 - q1._2).abs

  def benchmark(ctx: Unit): Solutions = {
    def placeQueens(n: Int): Solutions = n match {
      case _ if n == 0 => List(Nil)
      case _ => for {
        queens <- placeQueens(n - 1)
        y <- 1 to size
        queen = (n, y)
        if (isSafe(queen, queens))
      } yield queen :: queens
    }
    placeQueens(size)
  }

  def setup(): Unit = ()

  val name: String = "N-Queens"

  def factorial(n: BigInt): BigInt = {
    if (n > 1)
      n * factorial(n-1)
    else
      1
  }
  
  val size: Int = factorial(N).toInt

  override def hash(results: Solutions): Int = results.toSet.##()

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x4588c03a,
      )
}
