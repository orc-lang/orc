{- savina_sieve.orc -- Orc program implementing Eratosthenes's sieve

This is based on the approach in the Savina benchmark suite.

The approach of this implementation is to build a chain of processes which
each handle some number of primes. This means that at the beginning there
is very little parallelism, but as the process continues it will increase.
This makes this a very interesting benchmark for testing the ability of 
the runtime to adjust to changing behavior.
-}

include "benchmark.inc"

import class Sieve = "orc.test.item.scalabenchmarks.Sieve"
import class SavinaSieve = "orc.test.item.scalabenchmarks.SavinaSieve"

import class Set = "java.util.Set"
import class HashSet = "java.util.HashSet"
import class ArrayList = "java.util.ArrayList"
import class Collections = "java.util.Collections"

val sieveFragementSize = 300

val counter = Counter()

def sieveFragment(outChan) =
	val inChan = Channel() 
	val list = ArrayList[Number](sieveFragementSize)
	val next = Cell()
	def filter(x) = 
		val v = SavinaSieve.check(list, x)
		v >true> (
			if list.size() <: sieveFragementSize then
				list.add(x) >> outChan.put(x)
			else
				-- create a new fragment
				(next.readD() ;
					next.write(sieveFragment(outChan))) >> stop |
				next.read().put(x)
		) |
		v >false> signal #
	repeat({ (inChan.get() ; next.readD().close() >> stop) >x> filter(x) }) >> stop |
	inChan
	
def sforBy(low, high, step, f) =
  def h(i) if (i >= high) = signal
  def h(i) = f(i) >> h(i + step)
  h(low)

def primes(Number) :: List[Number]
def primes(n) =
	val out = Channel() #
	(
	val filter = sieveFragment(out)
	sforBy(3, n, 2, filter.put) >> filter.close() >> stop
	) ; 2 : out.getAll()

val N = Sieve.N()

benchmarkSized("Sieve-savina-scala", N, { signal }, { _ >> primes(N) }, Sieve.check)

{-
BENCHMARK
-}
