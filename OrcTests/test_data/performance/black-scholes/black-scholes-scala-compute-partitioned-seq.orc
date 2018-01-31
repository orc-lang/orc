{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute

def sforBy(low, high, step) = Sequentialize() >> (
  if low >= high then stop
  else ( low | sforBy(low+step, high, step) )
  )

def sfor(low, high) = Sequentialize() >> sforBy(low, high, 1)
  
  
val riskless = BlackScholesData.riskless()
val volatility = BlackScholesData.volatility()

def run(data) =
	val res = Array(data.length?)
	val partitionSize = Ceil((0.0 + data.length?) / nPartitions)
	forBy(0, data.length?, partitionSize) >partitionIndex> Sequentialize() >>
	  sfor(partitionIndex, min(partitionIndex + partitionSize, data.length?)) >i>
	  data(i)? >e>
	  res(i) := compute(e.price(), e.strike(), e.maturity(), riskless, volatility) 
	  >> stop ;
	Println(res.length?) >>
	Println((res(0)?, res(1)?, res(2)?, res(3)?, res(5)?))

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala-compute-partitioned-seq", data.length?, { data }, run)

{-
BENCHMARK
-}
  