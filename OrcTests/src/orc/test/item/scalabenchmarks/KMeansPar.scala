package orc.test.item.scalabenchmarks

import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAdder

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
          println(r.mkString("\n"))
        }
      }
    }
  }


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
      val c: BigDecimal = counts(i).sum()
      new Point(xs(i).sum/c, ys(i).sum/c)
    }).toArray
  }
}