//
// SSSP.scala -- Scala benchmarks for SSSP
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.sssp

import java.util.ArrayList

import scala.collection.JavaConverters.asScalaBufferConverter

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig, ExpectedBenchmarkResult, HashBenchmarkResult }

/** Single-Source Shortest Path
  *
  * Implemented using BFS.
  *
  * This is a naive implementation which uses mutable arrays but is
  * otherwise not optimized. It does not implement edge weights since
  * non-weighted SSSP scales better using simple algorithms.
  */
object SSSP extends ExpectedBenchmarkResult[Array[Int]] {
  lazy val (nodes, edges, source) = {
    val (ns, es, src) = SSSPData.generate(BenchmarkConfig.problemSizeSqrtScaledInt(80000))
    (ns, es.map(_.to), src)
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x18e8f495,
      10 -> 0xb6304a01,
      100 -> 0x55e0917b,
      )
}

abstract class SSSPBase extends BenchmarkApplication[(Array[SSSPNode], Array[Int], Int), Array[Int]] with HashBenchmarkResult[Array[Int]] {
  def setup(): (Array[SSSPNode], Array[Int], Int) = {
    import SSSP._
    (nodes, edges, source)
  }

  lazy val size: Int = SSSP.nodes.length * SSSP.nodes.length

  val expected = SSSP
}

object SSSPSeq extends SSSPBase {
  val name: String = "SSSP-seq"

  def benchmark(ctx: (Array[SSSPNode], Array[Int], Int)) = {
    val (nodes, edges, source) = ctx
    ssspSeq(nodes, edges, source)
  }

  def ssspSeq(nodes: Array[SSSPNode], edges: Array[Int], source: Int): Array[Int] = {
    val visited = Array.fill(nodes.size)(false)
    val result = Array.fill(nodes.size)(Int.MaxValue)
    val queue = collection.mutable.Queue[Int]()
    result(source) = 0
    queue.enqueue(source)
    while (queue.nonEmpty) {
      val index = queue.dequeue()

      for (to <- nodes(index).edges(edges)) {
        if (!visited(to)) {
          result(to) = result(index) + 1
          visited(to) = true
          queue.enqueue(to)
        }
      }
    }
    result
  }
}

object SSSPBatched extends SSSPBase {
  val name: String = "SSSP-batched"

  def benchmark(ctx: (Array[SSSPNode], Array[Int], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatched(nodes, edges, source)
  }

  def ssspBatched(nodes: Array[SSSPNode], edges: Array[Int], source: Int): Array[Int] = {
    val visited = Array.fill(nodes.size)(false)
    val result = Array.fill(nodes.size)(Int.MaxValue)
    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0

    result(source) = 0
    batches(batchNumber).add(source)

    while (! batches(batchNumber).isEmpty()) {
      val inQueue = batches(batchNumber)
      val outQueue = batches((batchNumber + 1) % 2)

      for (index <- inQueue.asScala) {
        @inline def currentCost = result(index)

        for (to <- nodes(index).edges(edges)) {
          if (!visited(to)) {
            result(to) = currentCost + 1
            visited(to) = true
            outQueue.add(to)
          }
        }
      }

      inQueue.clear()

      batchNumber = (batchNumber + 1) % 2
    }
    result
  }

}

// FIXME: Has contention issue. See line 186.

object SSSPBatchedPar extends SSSPBase {
  val name: String = "SSSP-batched-par"

  def benchmark(ctx: (Array[SSSPNode], Array[Int], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatchedPar(nodes, edges, source)
  }

  // Lines: 17
  def ssspBatchedPar(nodes: Array[SSSPNode], edges: Array[Int], source: Int): Array[Int] = {
    val visited = Array.fill(nodes.size)(false)
    val result = Array.fill(nodes.size)(Int.MaxValue)

    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0

    result(source) = 0
    batches(batchNumber).add(source)

    while (! batches(batchNumber).isEmpty()) {
      val inQueue = batches(batchNumber)
      val outQueue = batches((batchNumber + 1) % 2)

      for (queueIndex <- (0 until inQueue.size).par) {
        val index = inQueue.get(queueIndex)

        for (edgeIndex <- nodes(index).initialEdge until nodes(index).initialEdge + nodes(index).nEdges) {
          processEdge(edges, visited, result, edgeIndex, result(index), outQueue)
        }
      }

      inQueue.clear()

      batchNumber = (batchNumber + 1) % 2
    }
    result
  }

  // Lines: 7
  def processEdge(edges: Array[Int], visited: Array[Boolean], result: Array[Int], edgeIndex: Int, currentCost: Int, outQueue: ArrayList[Int]) = {
    val to = edges(edgeIndex)
    if (!visited(to)) {
      visited(to) = true
      result(to) = currentCost + 1

      outQueue synchronized {
        outQueue.add(to)
      }
    }
  }

  // Lines: 7
  def processEdge(edges: Array[Int], visited: ArrayList[Object], result: ArrayList[Object], edgeIndex: Int, currentCost: Int, outQueue: ArrayList[Object]) = {
    val to = edges(edgeIndex)
    if (!visited.get(to).asInstanceOf[Boolean]) {
      visited.set(to, true.asInstanceOf[AnyRef])
      result.set(to, (currentCost.asInstanceOf[Number].longValue() + 1).asInstanceOf[AnyRef])

      outQueue synchronized {
        outQueue.add(to.asInstanceOf[AnyRef])
      }
    }
  }
}

object SSSPBatchedParManual extends SSSPBase {
  val name: String = "SSSP-batched-par-manual"

  def benchmark(ctx: (Array[SSSPNode], Array[Int], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatchedPar(nodes, edges, source)
  }

  def ssspBatchedPar(nodes: Array[SSSPNode], edges: Array[Int], source: Int): Array[Int] = {
    val visited = Array.fill(nodes.size)(false)
    val result = Array.fill(nodes.size)(Int.MaxValue)

    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0

    result(source) = 0
    batches(batchNumber).add(source)

    while (! batches(batchNumber).isEmpty()) {
      val inQueue = batches(batchNumber)
      val outQueue = batches((batchNumber + 1) % 2)

      val stride = (inQueue.size / 8) max 256

      for (qindexStart <- (0 until inQueue.size by stride).par) {
        val qindexEnd = (qindexStart + stride) min inQueue.size
        val localQ = new ArrayList[Int]()

        for (queueIndex <- qindexStart until qindexEnd) {
          val index = inQueue.get(queueIndex)

          for (edgeIndex <- nodes(index).initialEdge until nodes(index).initialEdge + nodes(index).nEdges) {
            SSSPBatchedPar.processEdge(edges, visited, result, edgeIndex, result(index), localQ)
          }
        }

        outQueue synchronized {
          outQueue.addAll(localQ)
        }
      }

      inQueue.clear()

      batchNumber = (batchNumber + 1) % 2
    }
    result
  }
}
