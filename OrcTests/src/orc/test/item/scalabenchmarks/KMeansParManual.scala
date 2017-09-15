package orc.test.item.scalabenchmarks

import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAdder

object KMeansParManual extends BenchmarkApplication {
  val n = 10
  val iters = 1

  import KMeans._
    
  
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
  
  def updateCentroids(data: List[Point], centroids: Seq[Point]): Seq[Point] = {
    val xs = Array.fill(centroids.size)(new DoubleAdder())
    val ys = Array.fill(centroids.size)(new DoubleAdder())
    val counts = Array.fill(centroids.size)(new LongAdder())
    for (partition <- split(8, data).par) {
      val lxs = Array.fill(centroids.size)(0.0)
      val lys = Array.fill(centroids.size)(0.0)
      val lcounts = Array.fill(centroids.size)(0)
      
      for (p <- partition) {
        val cluster = closestIndex(p, centroids)
        lxs(cluster) += p.x.toDouble
        lys(cluster) += p.y.toDouble
        lcounts(cluster) += 1
      }

      (xs zip lxs).foreach(p => p._1.add(p._2))
      (ys zip lys).foreach(p => p._1.add(p._2))
      (counts zip lcounts).foreach(p => p._1.add(p._2))
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