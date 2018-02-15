{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
import site Abs = "orc.lib.math.Abs"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val a1 = 0.31938153
val a2 = -0.356563782
val a3 = 1.781477937
val a4 = -1.821255978
val a5 = 1.330274429
val rsqrt2pi = BlackScholes.rsqrt2pi()


def abs(x) = Sequentialize() >> Abs(x)

def round(x) = Sequentialize() >> x.doubleValue()

-- The cumulative normal distribution function
def cnd(x) = Sequentialize() >> (
    val l = abs(x)
    val k = round(1.0 / (1.0 + 0.2316419 * l))
    val w = round(1.0 - rsqrt2pi * Exp(-l * l / 2) * (a1 * k + a2 * k*k + a3 * k*k*k + a4 * k*k*k*k + a5 * k*k*k*k*k))
    if x <: 0.0 then
      1.0 - w
    else
      w
    )

def compute(s, x, t, r, v) = Sequentialize() >> (
    val d1 = round((Log(s / x) + (r + v * v / 2) * t) / (v * (t ** 0.5)))
    val d2 = round(d1 - v * (t ** 0.5))
    --val _ = Println((d1.getClass(), d1, d2.getClass(), d2))

    val call = s * cnd(d1) - x * Exp(-r * t) * cnd(d2)
    val put = x * Exp(-r * t) * cnd(-d2) - s * cnd(-d1)
    
    BlackScholesResult(call, put)
    )

def sforBy(low, high, step) = Sequentialize() >> (
  if low >= high then stop
  else ( low | sforBy(low+step, high, step) )
  )

def sfor(low, high) = Sequentialize() >> sforBy(low, high, 1)
  
def run(data) =
	val res = Array(data.length?)
	val partitionSize = Ceil((0.0 + data.length?) / nPartitions)
	forBy(0, data.length?, partitionSize) >partitionIndex> Sequentialize() >>
	  sfor(partitionIndex, min(partitionIndex + partitionSize, data.length?)) >i>
	  res(i) := compute(data(i)?.price(), data(i)?.strike(), data(i)?.maturity(), BlackScholesData.riskless(), BlackScholesData.volatility()) 
	  >> stop ;
	res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-partitioned-seq", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  