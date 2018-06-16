//
// BigSort.scala -- Scala benchmark BigSort
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.util.Random

import scala.annotation.tailrec

// FIXME: Fix for merge to be simple, fast, and binary only.

object BigSortData extends ExpectedBenchmarkResult[IndexedSeq[Number]] {
  val arraySize = BenchmarkConfig.problemSizeScaledInt(1000)
  
  def makeRandomArray(n: Int): Array[Number] = {
    val rng = new Random(n)
    Array.fill(n)(rng.nextInt(n).asInstanceOf[Number])
  }

  implicit val numberOrdering: Ordering[Number] = Ordering.by(_.longValue())
  
  def sort(a: Array[Number], start: Int, length: Int): IndexedSeq[Number] = {
    a.slice(start, start + length).sorted
  }
  
  def mergeSorted(inputs: Seq[IndexedSeq[Number]]): IndexedSeq[Number] = {
    val indices = Array.fill(inputs.size)(0) 
    val outputLen = inputs.map(_.size).sum
    val output = Array.ofDim[Number](outputLen)

    def minIndex(): Option[Int] = {
      try {
        val (nextI, indexI) = indices.view.zipWithIndex
          .filter({ case (nextI, indexI) => nextI < inputs(indexI).size })
          .minBy({ case (nextI, indexI) => inputs(indexI)(nextI) })
  
        Some(indexI)
      } catch {
        case _: UnsupportedOperationException =>
          None
      }
    }
    
    def takeMinValue(): Option[Number] = {
      minIndex() map { i =>
        val x = inputs(i)(indices(i))
        indices(i) = indices(i) + 1
        x
      }
    }
    
    @tailrec
    def merge(i: Int): Unit = {
      takeMinValue() match {
        case Some(v) =>
          output(i) = v
          merge(i + 1)
        case None =>
          ()
      }
    }
    merge(0)
    
    output
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0x1c2ca2af,
      10 -> 0xc63f1164,
      100 -> 0x98189f1b,
      )
}

abstract class BigSortBase extends BenchmarkApplication[Array[Number], IndexedSeq[Number]] with HashBenchmarkResult[IndexedSeq[Number]] {
  import BigSortData._
  
  val expected = BigSortData  
  val nPartitions = BenchmarkConfig.nPartitions
  
  def splitSortMerge(input: Array[Number], sort: IndexedSeq[Number] => IndexedSeq[Number]): IndexedSeq[Number] = {
    val partitionSize = (input.size / nPartitions).floor.toInt
    val sortedPartitions = for (start <- 0 until input.size by partitionSize) yield {
      sort(input.slice(start, start + (partitionSize min (input.size - start))))
    }
    mergeSorted(sortedPartitions)
  }

  def splitSortMergePar(input: Array[Number], sort: IndexedSeq[Number] => IndexedSeq[Number]): IndexedSeq[Number] = {
    val partitionSize = (input.size / nPartitions).floor.toInt
    val sortedPartitions = for (start <- (0 until input.size by partitionSize).par) yield {
      sort(input.slice(start, start + (partitionSize min (input.size - start))))
    }
    mergeSorted(sortedPartitions.toIndexedSeq)
  }

  val size: Int = (arraySize.toDouble * math.log(arraySize)).toInt

  def setup(): Array[Number] = {
    BigSortData.makeRandomArray(arraySize)
  }
}

object BigSortSeq extends BigSortBase {
  import BigSortData._
  
  val name: String = "BigSort-seq"

  def benchmark(a: Array[Number]): IndexedSeq[Number] = {
    splitSortMerge(a, _.sorted)
  }
}

object BigSortPar extends BigSortBase {
  import BigSortData._
  
  val name: String = "BigSort-par"

  def benchmark(a: Array[Number]): IndexedSeq[Number] = {
    splitSortMergePar(a, _.sorted)
  }
}
