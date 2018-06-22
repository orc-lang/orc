{- bigsort.orc -- A parallel/distributed sort benchmark.
 - 
 -}

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"

def splitSortMerge(input) =
  val partitionSize = Floor(input.length? / nPartitions)
  val sortedPartitions = collect({
    forBy(0, input.length?, partitionSize) >start> (
      val sorted = BigSortData.sort(input, start, min(partitionSize, input.length? - start))
      sorted
    )
  })
  cfold(BigSortData.mergeSorted, sortedPartitions)

def setup() = BigSortData.makeRandomArray(BigSortData.arraySize())
  
benchmarkSized("BigSort-scala", BigSortData.arraySize() * Log(BigSortData.arraySize()), setup, { splitSortMerge(_) }, BigSortData.check) 


{-
BENCHMARK
-}
