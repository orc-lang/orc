{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"
include "porce-debug.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
import site SinglePublication = "orc.compile.orctimizer.SinglePublication"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

{--
@def forBy(Integer, Integer, Integer) :: Integer
<link linkend="ref.concepts.publish">Publish</link> all values in the given half-open range with are multiples of 
<code>step</code> from <code>low</code>, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 3 5
forBy(1,6,2)]]></programlisting>

@implementation
--}
def forBy(Integer, Integer, Integer) :: Integer
def forBy(low, high, step) = low >> high >> step >> (
  if low >= high then stop
  else ( low | low+step >x> forBy(x, high, step) )
)

{--
@def for(Integer, Integer) :: Integer
<link linkend="ref.concepts.publish">Publish</link> all values in the given half-open range, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 2 3 4 5
for(1,6)]]></programlisting>

@implementation
--}
def for(Integer, Integer) :: Integer
def for(low, high) = forBy(low, high, 1)

{--
@def upto(Integer) :: Integer
<code>upto(n)</code> <link linkend="ref.concepts.publish">publishes</link> all values in the range <code>(0..n-1)</code>
simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2 3 4
upto(5)]]></programlisting>

@implementation
--}
def upto(Integer) :: Integer
def upto(high) = for(0, high)

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
def forTree(low, high) = low >> high >> (
  if high - low <= 8 then 
  	--Println("for " + low + " to " + high) >>
  	for(low, high)
  else
    val split = low + (high - low) / 2 #
    split >>
  	( forTree(low, split) | forTree(split, high) )
)


val compute = BlackScholes.compute
  
def run(data) =
	val res = Array(data.length?)
	val riskless = BlackScholesData.riskless()
	val volatility = BlackScholesData.volatility()
	res >> forTree(0, data.length?) >i>
	  Sequentialize() >>
	  data(i)? >d>
	  res(i) := compute(d.price(), d.strike(), d.maturity(), riskless, volatility) >>
	  --TraceTask(i) >> 
	  stop ;
	res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala-compute-for-tree-opt", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  