//
// KMeansParManual.scala -- Scala benchmark KMeansParManual
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

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig, HashBenchmarkResult }

object KMeansParManual extends BenchmarkApplication[Array[Point], Array[Point]] with HashBenchmarkResult[Array[Point]] {
  val expected = KMeansData
  val nPartitions = BenchmarkConfig.nPartitions

  import KMeans._
    
  def benchmark(data: Array[Point]) = {
    run(data)
  }

  def setup(): Array[Point] = {
    KMeansData.data
  }
  
  val size: Int = KMeansData.data.size
  val name: String = "KMeans-par-manual"


  def run(xs: Array[Point]) = {
    var centroids: Array[Point] = xs take n

    for (i <- 1 to iters) {
      centroids = updateCentroids(xs, centroids)
    }
    centroids
  }
  
  def updateCentroids(data: Array[Point], centroids: Array[Point]): Array[Point] = {
    val xs = Array.fill(centroids.size)(new DoubleAdder())
    val ys = Array.fill(centroids.size)(new DoubleAdder())
    val counts = Array.fill(centroids.size)(new LongAdder())
    val partitionSize = (data.size.toDouble / nPartitions).ceil.toInt
    for (index <- (0 until data.size by partitionSize).par) {
      //println(s"Partition: $index to ${index + partitionSize} (${data.size})")
      val (lxs, lys, lcounts) = sumAndCountClusters(data, centroids, index, index + partitionSize min data.size)

      (xs zip lxs).foreach(p => p._1.add(p._2.toDouble))
      (ys zip lys).foreach(p => p._1.add(p._2.toDouble))
      (counts zip lcounts).foreach(p => p._1.add(p._2.toLong))
    }
    centroids.indices.map({ i =>
      val c: D = counts(i).sum()
      new Point(xs(i).sum/c, ys(i).sum/c)
    }).toArray
  }
}
