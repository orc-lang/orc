{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val a1 = 0.31938153
val a2 = -0.356563782
val a3 = 1.781477937
val a4 = -1.821255978
val a5 = 1.330274429
val rsqrt2pi = BlackScholes.rsqrt2pi()

def round(x) = x.doubleValue()

-- The cumulative normal distribution function
def cnd(x) =
    val l = abs(x)
    val k = round(1.0 / (1.0 + 0.2316419 * l))
    val w = round(1.0 - rsqrt2pi * Exp(-l * l / 2) * (a1 * k + a2 * k*k + a3 * k*k*k + a4 * k*k*k*k + a5 * k*k*k*k*k))
    if x <: 0.0 then
      1.0 - w
    else
      w

def compute(s, x, t, r, v) = 
    val d1 = round((Log(s / x) + (r + v * v / 2) * t) / (v * sqrt(t)))
    val d2 = round(d1 - v * sqrt(t))

    val call = s * cnd(d1) - x * Exp(-r * t) * cnd(d2)
    val put = x * Exp(-r * t) * cnd(-d2) - s * cnd(-d1)
    
    BlackScholesResult(call, put)
  
  
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
	val res = Array(data.length?)
	for(0, data.length?) >i>
	  res(i) := compute(data(i)?.price(), data(i)?.strike(), data(i)?.maturity(), riskless, volatility) 
	  >> stop ;
	res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  