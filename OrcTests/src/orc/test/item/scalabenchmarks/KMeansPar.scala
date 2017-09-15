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


  def run(xs: List[Point]) = {
    var centroids: Seq[Point] = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    centroids
  }
  
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
  
  def updateCentroids(data: List[Point], centroids: Seq[Point]): Seq[Point] = {
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
    }).toSeq
  }

  def closestIndex(x: Point, choices: Seq[Point]): Int = {
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
}