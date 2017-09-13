package orc.test.item.scalabenchmarks

object KMeansPar extends BenchmarkApplication {
  val n = 10
  val iters = 1
  
  def main(args: Array[String]): Unit = {
    if (args.size == 0) {
      val r = run(KMeansData.data)
      println(r.size)
    } else if (args.size == 1) {
      val n = args(0).toInt
      for (_ <- 0 until n) {
        Util.timeIt {
          val r = run(KMeansData.data)
          println(r.size)
        }
      }
    }
  }

  def run(xs: List[Point]) = {
    var centroids = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    //clusters(xs, centroids)
    centroids
  }
  
  def updateCentroids(data: List[Point], centroids: List[Point]): List[Point] = {
    clusters(data, centroids).par.map(average).toList
  }

  def clusters(xs: List[Point], centroids: List[Point]): List[List[Point]] =
    (xs.par groupBy { x => closest(x, centroids) }).values.map(_.toList).toList

  def closest(x: Point, choices: List[Point]) =
    choices minBy { y => dist(x, y) }

  def dist(x: Point, y: Point) = (x - y).modulus

  def average(xs: List[Point]) = xs.reduce(_ + _) / xs.size
}