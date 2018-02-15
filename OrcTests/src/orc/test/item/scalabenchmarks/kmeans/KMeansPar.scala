package orc.test.item.scalabenchmarks.kmeans

import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAdder
import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.Util
import orc.test.item.scalabenchmarks.HashBenchmarkResult

object KMeansPar extends BenchmarkApplication[Array[Point], Array[Point]] with HashBenchmarkResult[Array[Point]] {
  val expected = KMeansData
  val n = 10
  val iters = 1
  
  def benchmark(data: Array[Point]) = {
    run(data)
  }

  def setup(): Array[Point] = {
    KMeansData.data
  }
  
  val size: Int = KMeansData.data.size
  val name: String = "KMeans-par"


  def run(xs: Array[Point]) = {
    var centroids: Array[Point] = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    centroids
  }
  
  import KMeans._
  
  def updateCentroids(data: Array[Point], centroids: Array[Point]): Array[Point] = {
    val xs = Array.fill(centroids.size)(new DoubleAdder())
    val ys = Array.fill(centroids.size)(new DoubleAdder())
    val counts = Array.fill(centroids.size)(new LongAdder())
    for (p <- data.par) {
      val cluster = closestIndex(p, centroids)
      xs(cluster).add(p.x.toDouble)
      ys(cluster).add(p.y.toDouble)
      counts(cluster).add(1)
    }
    centroids.indices.map({ i =>
      val c: D = counts(i).sum()
      new Point(xs(i).sum/c, ys(i).sum/c)
    }).toArray
  }
}