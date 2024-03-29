{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute
  
  
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
    val res = Array(data.length?)
    for(0, data.length?) >i>
      res(i) := compute(data(i)?.price(), data(i)?.strike(), data(i)?.maturity(), riskless, volatility) 
      >> stop ;
    res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  