{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"
include "porce-debug.inc"

--import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
--import site SinglePublication = "orc.compile.orctimizer.SinglePublication"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

{--
@def for(Integer, Integer) :: Integer
<link linkend="ref.concepts.publish">Publish</link> all values in the given half-open range, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 2 3 4 5
for(1,6)]]></programlisting>

@implementation
--}
def forTree(Integer, Integer) :: Integer
def forTree(low, high) =
  if high - low <= 8 then 
  	--Println("for " + low + " to " + high) >>
  	for(low, high)
  else
    low + (high - low) / 2 >split>
  	( forTree(low, split) | forTree(split, high) )


val compute = BlackScholes.compute
  
def run(data) =
	val riskless = BlackScholesData.riskless()
	val volatility = BlackScholesData.volatility()
	val res = Array(data.length?)
	
	forTree(0, data.length?) >i>
	  res(i) := compute(data(i)?.price(), data(i)?.strike(), data(i)?.maturity(), riskless, volatility) >>
	  --data(i)? >d> res(i) := compute(d.price(), d.strike(), d.maturity(), riskless, volatility) >>
	  --TraceTask(i) >> 
	  stop ;
	res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala-compute-for-tree", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  