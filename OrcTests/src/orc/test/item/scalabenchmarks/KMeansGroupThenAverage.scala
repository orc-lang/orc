package orc.test.item.scalabenchmarks

import scala.io.Source
import scala.math.sqrt

object KMeansGroupThenAverage extends BenchmarkApplication {
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
          println(r.mkString("\n"))
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
    clusters(data, centroids) map average
  }

  def split(n: Int, xs: List[Point]): List[List[Point]] = {
    xs.grouped(xs.size / n).toList
  }
    
  def appendMultiple(ls: List[List[List[Point]]]) = {
    ls.reduce((x, y) => (x zip y).map({ case (a, b) => a ++ b }))
  }
    
  def clusters(xs: List[Point], centroids: List[Point]) =
    (xs groupBy { x => closest(x, centroids) }).values.toList

  def closest(x: Point, choices: List[Point]) =
    choices minBy { y => dist(x, y) }

  def dist(x: Point, y: Point) = (x - y).modulus

  def average(xs: List[Point]) = xs.reduce(_ + _) / xs.size
  def sum(xs: List[Point]) = xs.reduce(_ + _)
}