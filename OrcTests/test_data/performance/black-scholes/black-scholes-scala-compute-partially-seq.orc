{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute
  
  
def run(data) =
	val res = Array(data.length?)
	for(0, data.length?) >i> Sequentialize() >>
	  res(i) := compute(data(i)?.price(), data(i)?.strike(), data(i)?.maturity(), BlackScholesData.riskless(), BlackScholesData.volatility()) 
	  >> stop ;
	Println(res.length?) >>
	Println((res(0)?, res(1)?, res(2)?, res(3)?, res(5)?))


val dataSize = problemSizeScaledInt(100000)

val data = BlackScholesData.makeData(dataSize)

benchmarkSized("Black-Scholes-scala-compute-partially-seq", data.length?, { data }, run)

{-
BENCHMARK
-}
  