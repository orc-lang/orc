//
// KMeansPar.scala -- Scala benchmark KMeansPar
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.kmeans

import java.util.concurrent.atomic.{ DoubleAdder, LongAdder }

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, HashBenchmarkResult }

object KMeansPar extends BenchmarkApplication[Array[Point], Array[Point]] with HashBenchmarkResult[Array[Point]] {
  val expected = KMeansData

  def benchmark(data: Array[Point]) = {
    run(data)
  }

  def setup(): Array[Point] = {
    KMeansData.data
  }

  lazy val size: Int = KMeansData.data.size
  val name: String = "KMeans-par"

  import KMeans._

  // Lines: 4
  def run(xs: Array[Point]) = {
    var centroids: Array[Point] = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    centroids
  }

  // Lines: 13
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
