package orc.scalabenchmarks

import math.sqrt

import orc.test.BenchmarkApplication

import scala.io.Source

object KMeansData  {
  def readPoints(path: String): List[KMeans.Point] = {
    val json = Source.fromFile(path).mkString
    orc.lib.web.ReadJSON(json).asInstanceOf[List[List[BigDecimal]]].map({ case List(a, b) => new KMeans.Point(a, b) })
  }

  val data = readPoints("test_data/performance/points-small.json")
}

object KMeans extends BenchmarkApplication {
  val n = 10
  val iters = 15
  
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

  case class Point(x: BigDecimal, y: BigDecimal) {
    def /(k: Double): Point = new Point(x / k, y / k)

    def +(p: Point) = new Point(x + p.x, y + p.y)
    def -(p: Point) = new Point(x - p.x, y - p.y)

    def modulus: BigDecimal = sqrt((sq(x) + sq(y)).toDouble)
  }

  def run(xs: List[Point]) = {
    var centroids = xs take n

    for (i <- 1 to iters) {
      centroids = clusters(xs, centroids) map average
    }
    clusters(xs, centroids)
  }

  def clusters(xs: List[Point], centroids: List[Point]) =
    (xs groupBy { x => closest(x, centroids) }).values.toList

  def closest(x: Point, choices: List[Point]) =
    choices minBy { y => dist(x, y) }

  def sq(x: BigDecimal) = x * x

  def dist(x: Point, y: Point) = (x - y).modulus

  def average(xs: List[Point]) = xs.reduce(_ + _) / xs.size
}