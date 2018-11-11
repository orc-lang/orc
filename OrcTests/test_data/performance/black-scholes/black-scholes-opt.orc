{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
import site Abs = "orc.lib.math.Abs"

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

-- Lines: 8
def compute(option, r, v) = Sequentialize() >> ( -- Inferable (propogation from cnd)
    val (s, x, t) = (option.price(), option.strike(), option.maturity())
    round((Log(s / x) + (r + v * v / 2) * t) / (v * sqrt(t))) >d1>
    round(d1 - v * sqrt(t)) >d2>

    s * cnd(d1) - x * Exp(-r * t) * cnd(d2) >call>
    x * Exp(-r * t) * cnd(-d2) - s * cnd(-d1) >put>
    
    (option.setCall(call) |
    option.setPut(put))
    )
  

-- Lines: 8
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
	riskless >> volatility >>
	for(0, data.length?) >i> Sequentialize() >> -- Inferable (by propogation from compute)
	  compute(data(i)?, riskless, volatility) >> stop ;
	data


val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-opt", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  