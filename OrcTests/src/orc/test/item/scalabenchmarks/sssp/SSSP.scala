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
import java.util.concurrent.atomic.AtomicIntegerArray

import scala.collection.JavaConverters.asScalaBufferConverter

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig, ExpectedBenchmarkResult, HashBenchmarkResult }

/** Single-Source Shortest Path
  *
  * Implemented using BFS.
  *
  * This implementation is based on the implementation in Parboil
  * (http://impact.crhc.illinois.edu/parboil/parboil.aspx), which uses
  * a queue instead of marking (as in the Rodinia version).
  *
  * This is a naive implementation which uses mutable arrays but is
  * otherwise not optimized.
  */
object SSSP extends ExpectedBenchmarkResult[Array[Int]] {
  lazy val (nodes, edges, source) = SSSPData.generate(BenchmarkConfig.problemSizeSqrtScaledInt(40000))

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0xac4404ef,
      10 -> 0xddd2d72e,
      100 -> 0x3544edc6,
      )
}

abstract class SSSPBase extends BenchmarkApplication[(Array[SSSPNode], Array[SSSPEdge], Int), Array[Int]] with HashBenchmarkResult[Array[Int]] {
  def setup(): (Array[SSSPNode], Array[SSSPEdge], Int) = {
    import SSSP._
    (nodes, edges, source)
  }

  lazy val size: Int = (SSSP.nodes.length / 100000) * SSSP.nodes.length

  val expected = SSSP
}

object SSSPSeq extends SSSPBase {
  val name: String = "SSSP-seq"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)) = {
    val (nodes, edges, source) = ctx
    ssspSeq(nodes, edges, source)
  }
  
  def ssspSeq(nodes: Array[SSSPNode], edges: Array[SSSPEdge], source: Int): Array[Int] = {
    val colors = Array.fill(nodes.size)(0)
    var gray = 1
    val result = Array.fill(nodes.size)(Int.MaxValue)
    val queue = collection.mutable.Queue[Int]()
    result(source) = 0
    queue.enqueue(source)
    while (queue.nonEmpty) {
      val index = queue.dequeue()
      val currentCost = result(index)

      for (SSSPEdge(to, cost) <- nodes(index).edges(edges)) {
        val newCost = currentCost + cost
        if (newCost < result(to)) {
          result(to) = newCost
          if (colors(to) != gray) {
            colors(to) = gray
            queue.enqueue(to)
          }
        }
      }

      gray += 1
    }
    result
  }
}
  
object SSSPBatched extends SSSPBase {
  val name: String = "SSSP-batched"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatched(nodes, edges, source)
  }

  def ssspBatched(nodes: Array[SSSPNode], edges: Array[SSSPEdge], source: Int): Array[Int] = {
    val colors = Array.fill(nodes.size)(0)
    var gray = 1
    val result = Array.fill(nodes.size)(Int.MaxValue)
    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0
    
    result(source) = 0
    batches(batchNumber).add(source)
    
    while (! batches(batchNumber).isEmpty()) {
      val inQueue = batches(batchNumber)
      val outQueue = batches((batchNumber + 1) % 2)
      
      for (index <- inQueue.asScala) {
        val currentCost = result(index)
  
        for (SSSPEdge(to, cost) <- nodes(index).edges(edges)) {
          val newCost = currentCost + cost
          if (newCost < result(to)) {
            result(to) = newCost
            if (colors(to) != gray) {
              colors(to) = gray
              outQueue.add(to)
            }
          }
        }
      }
      
      inQueue.clear()

      gray += 1
      batchNumber = (batchNumber + 1) % 2
    }
    result
  }
  
}
 
// FIXME: Has contention issue. See line 186.

object SSSPBatchedPar extends SSSPBase {
  val name: String = "SSSP-batched-par"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatchedPar(nodes, edges, source)
  }


  def ssspBatchedPar(nodes: Array[SSSPNode], edges: Array[SSSPEdge], source: Int): Array[Int] = {
    val colors = new AtomicIntegerArray(nodes.size)
    var gray = 1
    val result = new AtomicIntegerArray(nodes.size)
    
    for (i <- 0 until result.length()) { 
      result.set(i, Int.MaxValue)
    }
    
    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0
    
    result.set(source, 0)
    batches(batchNumber).add(source)
    
    while (! batches(batchNumber).isEmpty()) {
      val inQueue = batches(batchNumber)
      val outQueue = batches((batchNumber + 1) % 2)
      
      for (queueIndex <- (0 until inQueue.size).par) {
        val index = inQueue.get(queueIndex)
        val currentCost = result.get(index)
  
        for (edgeIndex <- nodes(index).initialEdge until nodes(index).initialEdge + nodes(index).nEdges) {
          processEdge(edges, colors, result, gray, edgeIndex, currentCost, outQueue)
        }
      }
      
      inQueue.clear()

      gray += 1
      batchNumber = (batchNumber + 1) % 2
    }
    (0 until result.length()).map(result.get(_)).toArray
  }
  
  def processEdge(edges: Array[SSSPEdge], colors: AtomicIntegerArray, result: AtomicIntegerArray, gray: Int, edgeIndex: Int, currentCost: Int, outQueue: ArrayList[Int]) = {
    val SSSPEdge(to, cost) = edges(edgeIndex)
    val newCost = currentCost + cost
    // FIXME: This next line is a major source of contention.
    val oldCost = result.getAndAccumulate(to, newCost, _ min _)
    if (newCost < oldCost) {
      val oldColor = colors.getAndSet(to, gray)
      if (oldColor != gray) {
        outQueue synchronized {
          outQueue.add(to)
        }
      }
    }
  }
}

object SSSPBatchedParManual extends SSSPBase {
  val name: String = "SSSP-batched-par-manual"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)) = {
    val (nodes, edges, source) = ctx
    ssspBatchedPar(nodes, edges, source)
  }


  def ssspBatchedPar(nodes: Array[SSSPNode], edges: Array[SSSPEdge], source: Int): Array[Int] = {
    val colors = new AtomicIntegerArray(nodes.size)
    var gray = 1
    val result = new AtomicIntegerArray(nodes.size)
    
    for (i <- 0 until result.length()) { 
      result.set(i, Int.MaxValue)
    }
    
    val batches = (0 until 2).map(_ => new ArrayList[Int]()).toArray
    var batchNumber = 0
    
    result.set(source, 0)
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
          val currentCost = result.get(index)
    
          for (edgeIndex <- nodes(index).initialEdge until nodes(index).initialEdge + nodes(index).nEdges) {
            val SSSPEdge(to, cost) = edges(edgeIndex) 
            val newCost = currentCost + cost
            val oldCost = result.getAndAccumulate(to, newCost, _ min _)
            if (newCost < oldCost) {
              val oldColor = colors.getAndSet(to, gray)
              if (oldColor != gray) {
                localQ.add(to)
              }
            }
          }
        }
        
        outQueue synchronized {
          outQueue.addAll(localQ)
        }
      }
      
      inQueue.clear()

      gray += 1
      batchNumber = (batchNumber + 1) % 2
    }
    (0 until result.length()).map(result.get(_)).toArray
  }
}
