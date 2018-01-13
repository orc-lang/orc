package orc.test.item.scalabenchmarks

import scala.annotation.tailrec
import java.util.concurrent.ThreadLocalRandom

abstract class BigSortBase extends BenchmarkApplication[Array[Number]] {
  implicit val numberOrdering: Ordering[Number] = Ordering.by(_.longValue())
  
  def mergeSorted(inputs: IndexedSeq[IndexedSeq[Number]]): IndexedSeq[Number] = {
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

  def makeRandomArray(n: Int): Array[Number] = {
    Array.fill(n)(ThreadLocalRandom.current().nextInt(n).asInstanceOf[Number])
  }
  
  val arraySize = BenchmarkConfig.problemSizeScaledInt(1000)

  
  val name: String = "BigSort"

  val size: Int = (arraySize.toDouble * math.log(arraySize)).toInt

  def setup(): Array[Number] = {
    makeRandomArray(arraySize)
  }
}

object BigSortSeq extends BigSortBase {
  def benchmark(a: Array[Number]): Unit = {
    splitSortMerge(a, _.sorted)
  }
}

object BigSortPar extends BigSortBase {
  def benchmark(a: Array[Number]): Unit = {
    splitSortMergePar(a, _.sorted)
  }
}