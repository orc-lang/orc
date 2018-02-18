package orc.test.item.scalabenchmarks.kmeans

import scala.io.Source
import scala.math.sqrt

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig, ExpectedBenchmarkResult, HashBenchmarkResult }

import KMeans.D



object KMeansData extends ExpectedBenchmarkResult[Array[Point]] {
  def readPoints(path: String): Array[Point] = {
    val json = Source.fromFile(path).mkString
    val data = orc.lib.web.ReadJSON(json).asInstanceOf[List[List[Number]]].map({ case List(a, b) => new Point(a.doubleValue, b.doubleValue) })
    data.toArray
  }

  lazy val dataBase = readPoints(s"${System.getProperty("orc.test.benchmark.datadir", "")}test_data/performance/points.json")
  
  def data = dataSized((BenchmarkConfig.problemSize / 3.0).ceil.toInt)
  
  private def dataSized(n: Int) = (0 until n).foldLeft(Array[Point]())((acc, _) => acc ++ dataBase)

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x83731af5,
      10 -> 0x83731af5,
      100 -> 0x83731af5,
      )
  
  // println(s"Loaded ${dataBase.size} points from JSON")
}

case class Point(val x: D, val y: D) {
  def /(k: D): Point = new Point(x / k, y / k)

  def +(p: Point) = new Point(x + p.x, y + p.y)
  def -(p: Point) = new Point(x - p.x, y - p.y)
  
  def add(p: Point) = this + p
  def sub(p: Point) = this - p
  def div(k: Int) = this / k

  def modulus: D = sqrt((sq(x) + sq(y)).toDouble)
  
  def sq(x: D) = x * x
  
  override def hashCode(): Int = {
    val prec = 1e6
    (x * prec).toLong.## * 37 ^ (y * prec).toLong.## 
  }
}

object KMeans extends BenchmarkApplication[Array[Point], Array[Point]] with HashBenchmarkResult[Array[Point]] {
  val expected = KMeansData
  
  type D = Double
  object D {
    def apply(x: Integer): Double = x.toDouble
    def apply(x: Double): Double = x
    def valueOf(x: Double): Double = x    
  }
  
  val n = 10
  val iters = 1

  def benchmark(data: Array[Point]) = {
    run(data)
  }

  def setup(): Array[Point] = {
    KMeansData.data
  }
  
  val size: Int = KMeansData.data.size
  val name: String = "KMeans"

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
    val xs = Array.fill[D](centroids.size)(D(0))
    val ys = Array.fill[D](centroids.size)(D(0))
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
      val c: D = D(counts(i))
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
    var closestDist = D(0)
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