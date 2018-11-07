{- bigsort.orc -- A parallel/distributed sort benchmark.
 - 
 -}

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"

-- Lines: 8
def splitSortMerge(input) = input >>
  Floor(input.length? / nPartitions) >partitionSize>
  collect({
    forBy(0, input.length?, partitionSize) >start> Sequentialize() >> ( -- Inferable
      val sorted = BigSortData.sort(input, start, min(partitionSize, input.length? - start))
      sorted
    )
    
  }) >sortedPartitions>
  cfold(BigSortData.mergeSorted, sortedPartitions)

def setup() = BigSortData.makeRandomArray(BigSortData.arraySize())
  
benchmarkSized("BigSort-scala-opt", BigSortData.arraySize() * Log(BigSortData.arraySize()), setup, { splitSortMerge(_) }, BigSortData.check) 


{-
BENCHMARK
-}
