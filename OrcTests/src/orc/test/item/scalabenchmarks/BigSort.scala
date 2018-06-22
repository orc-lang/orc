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

object BigSortData extends ExpectedBenchmarkResult[IndexedSeq[Number]] {
  // TODO: We may need to be increased by 10x.
  val arraySize = BenchmarkConfig.problemSizeScaledInt(10000)
  
  def makeRandomArray(n: Int): Array[Number] = {
    val rng = new Random(n)
    Array.fill(n)(rng.nextInt(n).asInstanceOf[Number])
  }

  implicit val numberOrdering: Ordering[Number] = Ordering.by(_.longValue())
  
  def sort(a: Array[Number], start: Int, length: Int): IndexedSeq[Number] = {
    a.slice(start, start + length).sorted
  }

  def mergeSortedArray(a: Array[Any], b: Array[Any]): Array[Number] = {
    mergeSorted(a.seq.asInstanceOf[IndexedSeq[Number]], b.seq.asInstanceOf[IndexedSeq[Number]]).toArray
  }
  
  def mergeSorted(a: IndexedSeq[Number], b: IndexedSeq[Number]): IndexedSeq[Number] = {
    val outputLen = a.length + b.length
    val output = Array.ofDim[Number](outputLen)
    
    var aI = 0
    var bI = 0
    var oI = 0
    
    val ordering = implicitly[Ordering[Number]]
    
    while(oI < outputLen) {
      if (aI < a.length && (bI >= b.length || ordering.lt(a(aI), b(bI)))) {
        output(oI) = a(aI)
        aI += 1
      } else if (bI < b.length) {
        output(oI) = b(bI)
        bI += 1
      } else {
        assert(false)
      }
      oI += 1
    }
    output
  }

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0xc63f1164,
      10 -> 0x98189f1b,
      100 -> 0xaa7a1f70,
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
    sortedPartitions.reduce(mergeSorted)
  }

  def splitSortMergePar(input: Array[Number], sort: IndexedSeq[Number] => IndexedSeq[Number]): IndexedSeq[Number] = {
    val partitionSize = (input.size / nPartitions).floor.toInt
    val sortedPartitions = for (start <- (0 until input.size by partitionSize).par) yield {
      sort(input.slice(start, start + (partitionSize min (input.size - start))))
    }
    sortedPartitions.reduce(mergeSorted)
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
