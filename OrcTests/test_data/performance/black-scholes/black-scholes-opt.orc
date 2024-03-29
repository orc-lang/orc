{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
import site Abs = "orc.lib.math.Abs"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

-- Lines: 6
val a1 = 0.31938153
val a2 = -0.356563782
val a3 = 1.781477937
val a4 = -1.821255978
val a5 = 1.330274429
val rsqrt2pi = BlackScholes.rsqrt2pi()

def abs(x) = Abs(x)

def round(x) = Sequentialize() >> x.doubleValue() -- Inferable

-- Lines: 8
-- The cumulative normal distribution function
def cnd(x) = Sequentialize() >> ( -- Inferable
    abs(x) >l>
    round(1.0 / (1.0 + 0.2316419 * l)) >k>
    round(1.0 - rsqrt2pi * Exp(-l * l / 2) * (a1 * k + a2 * k*k + a3 * k*k*k + a4 * k*k*k*k + a5 * k*k*k*k*k)) >w> (
    if x <: 0.0 then
      1.0 - w
    else
      w
    ))

-- Lines: 6
def compute(s, x, t, r, v) = Sequentialize() >> ( -- Inferable (propogation from cnd)
    round((Log(s / x) + (r + v * v / 2) * t) / (v * sqrt(t))) >d1>
    round(d1 - v * sqrt(t)) >d2>

    s * cnd(d1) - x * Exp(-r * t) * cnd(d2) >call>
    x * Exp(-r * t) * cnd(-d2) - s * cnd(-d1) >put>
    
    BlackScholesResult(call, put)
    )
  

-- Lines: 9  
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
	val res = Array(data.length?)
	riskless >> volatility >> res >>
	for(0, data.length?) >i> Sequentialize() >> -- Inferable (by propogation from compute)
	  data(i)? >option>
	  res(i) := compute(option.price(), option.strike(), option.maturity(), riskless, volatility) 
	  >> stop ;
	res


val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-opt", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  