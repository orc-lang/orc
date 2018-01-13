package orc.test.item.scalabenchmarks.sssp

import java.util.ArrayList

import scala.collection.JavaConverters._
import java.util.concurrent.atomic.AtomicIntegerArray
import orc.test.item.scalabenchmarks._

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
object SSSP {
  val (nodes, edges, source) = SSSPData.generate(BenchmarkConfig.problemSizeSqrtScaledInt(40000))
}

abstract class SSSPBase extends BenchmarkApplication[(Array[SSSPNode], Array[SSSPEdge], Int)] {
  def setup(): (Array[SSSPNode], Array[SSSPEdge], Int) = {
    import SSSP._
    (nodes, edges, source)
  }

  val size: Int = (SSSP.nodes.length / 100000) * SSSP.nodes.length
}

object SSSPSeq extends SSSPBase {
  val name: String = "SSSP-seq"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)): Unit = {
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

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)): Unit = {
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
  
object SSSPBatchedPar extends SSSPBase {
  val name: String = "SSSP-batched-par"

  def benchmark(ctx: (Array[SSSPNode], Array[SSSPEdge], Int)): Unit = {
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