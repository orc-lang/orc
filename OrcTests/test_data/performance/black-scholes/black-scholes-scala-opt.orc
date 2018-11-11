{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute
  
-- Lines: 8
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
    riskless >> volatility >>
    for(0, data.length?) >i> Sequentialize() >> -- Inferable (by propogation from compute)
      compute(data(i)?, riskless, volatility) >> stop ;
    data

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala-opt", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  