package orc.test.item.scalabenchmarks

import scala.io.Source
import scala.math.sqrt

object KMeansData  {
  def readPoints(path: String): Array[Point] = {
    val json = Source.fromFile(path).mkString
    val data = orc.lib.web.ReadJSON(json).asInstanceOf[List[List[BigDecimal]]].map({ case List(a, b) => new Point(a, b) })
    data.toArray ++ data ++ data
  }

  val data = readPoints("test_data/performance/points.json")
  
  println(data.size)
}

case class Point(val x: BigDecimal, val y: BigDecimal) {
  def /(k: BigDecimal): Point = new Point(x / k, y / k)

  def +(p: Point) = new Point(x + p.x, y + p.y)
  def -(p: Point) = new Point(x - p.x, y - p.y)
  
  def add(p: Point) = this + p
  def sub(p: Point) = this - p
  def div(k: Int) = this / k

  def modulus: BigDecimal = sqrt((sq(x) + sq(y)).toDouble)
  
  def sq(x: BigDecimal) = x * x
}

object KMeans extends BenchmarkApplication {
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

  def run(xs: Array[Point]) = {
    var centroids = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    centroids
  }
  
  def takePointArray(n: Int, xs: Array[Point]) = xs take n
  
  /*
  def updateCentroids(data: List[Point], centroids: List[Point]): List[Point] = {
    val state = new collection.mutable.HashMap() ++ centroids.map(c => (c, (0.0, 0.0, 0)))
    for (p <- data) {
      val cluster = closest(p, centroids)
      val (x, y, count) = state(cluster)
      state += cluster -> ((x + p.x.toDouble, y + p.y.toDouble, count + 1))
    }
    state.values.map({ case (x, y, count) => Point(x/count, y/count) }).toList
  }
  */

  def sumAndCountClusters(data: Array[Point], centroids: Array[Point], start: Int, end: Int) = {
    val xs = Array.fill[BigDecimal](centroids.size)(BigDecimal(0))
    val ys = Array.fill[BigDecimal](centroids.size)(BigDecimal(0))
    val counts = Array.ofDim[Integer](centroids.size)
    var i = start
    while(i < end) {
      val p = data(i)
      val cluster = closestIndex(p, centroids)
      xs(cluster) += p.x
      ys(cluster) += p.y
      counts(cluster) += 1
      i += 1
    }
    (xs, ys, counts)
  }
  
  def updateCentroids(data: Array[Point], centroids: Array[Point]): Array[Point] = 
    updateCentroids(data, centroids, 0, data.size)
    
  def updateCentroids(data: Array[Point], centroids: Array[Point], start: Int, end: Int): Array[Point] = {
    val (xs, ys, counts) = sumAndCountClusters(data, centroids, start, end)
    centroids.indices.map({ i =>
      val c: BigDecimal = BigDecimal(counts(i))
      new Point(xs(i)/c, ys(i)/c)
    }).toArray
  }

  /*
  def split(n: Int, xs: Array[Point]): Array[Array[Point]] = {
    xs.grouped(xs.size / n).toArray
  }
    
  def appendMultiple(ls: Array[Array[Array[Point]]]) = {
    ls.reduce((x, y) => (x zip y).map({ case (a, b) => a ++ b }))
  }
  */
    
  def clusters(xs: Array[Point], centroids: Array[Point]) =
    (xs groupBy { x => closest(x, centroids) }).values.toList

  def closest(x: Point, choices: Array[Point]) =
    choices minBy { y => dist(x, y) }

  def closestIndex(x: Point, choices: Array[Point]): Int = {
    var index = 0
    var closestIndex = -1
    var closestDist = BigDecimal(0)
    for(y <- choices) {
      val d = dist(x, y)
      if(closestIndex < 0 || d < closestDist) {
        closestDist = d
        closestIndex = index
      }
      index += 1
    }
    closestIndex
  }

  def dist(x: Point, y: Point) = (x - y).modulus

  def average(xs: Array[Point]) = xs.reduce(_ + _) / xs.size
  def sum(xs: Array[Point]) = xs.reduce(_ + _)
}