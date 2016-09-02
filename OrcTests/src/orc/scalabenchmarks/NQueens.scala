package orc.scalabenchmarks

// From: https://gist.github.com/ornicar/1115259

// Solves the n-queens problem for an arbitrary board size
// Run for a board size of ten: scala nqueen.scala 10
object NQueens {
  type Queen = (BigInt, BigInt)
  type Solutions = List[List[Queen]]

  def main(args: Array[String]) {
    val size: Int = args match {
      case Array() => sys.error("Provide a board size")
      case Array(n) => n.toInt
    }
    def placeQueens(n: BigInt): Solutions = n match {
      case _ if n == 0  => List(Nil)
      case _ => for {
          queens <- placeQueens(n -1)
          y <- 1 to size
          queen = (n, BigInt(y))
          if (isSafe(queen, queens))
        } yield queen :: queens
      }
    val solutions = Util.timeIt { placeQueens(size) }
    println(solutions.size + " solutions found")
    // print the board of the first solution
    for (queen <- solutions.head; x <- 1 to size) {
      if (queen._2 == x) print("Q ") else print(". ")
      if (x == size) println()
    }
  }

  def isSafe(queen: Queen, others: List[Queen]) =
    others forall (!isAttacked(queen, _))

  def isAttacked(q1: Queen, q2: Queen) =
    q1._1 == q2._1 ||
    q1._2 == q2._2 ||
    (q2._1-q1._1).abs == (q2._2-q1._2).abs
}