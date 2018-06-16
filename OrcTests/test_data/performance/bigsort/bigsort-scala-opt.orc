{- bigsort.orc -- A parallel/distributed sort benchmark.
 - 
 -}

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"

-- FIXME: Fix for binary only merge.

def splitSortMerge(input) =
  val partitionSize = Floor(input.length? / nPartitions)
  val sortedPartitions = collect({
    forBy(0, input.length?, partitionSize) >start> Sequentialize() >> ( -- Inferable
      val sorted = BigSortData.sort(input, start, min(partitionSize, input.length? - start))
      sorted
    )
  })
  BigSortData.mergeSorted(sortedPartitions)

def setup() = BigSortData.makeRandomArray(BigSortData.arraySize())
  
benchmarkSized("BigSort-scala-opt", BigSortData.arraySize() * Log(BigSortData.arraySize()), setup, { splitSortMerge(_) }, BigSortData.check) 


{-
BENCHMARK
-}
