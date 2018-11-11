{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute
  
  
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
    for(0, data.length?) >i>
      compute(data(i)?, riskless, volatility) >> stop ;
    data

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  