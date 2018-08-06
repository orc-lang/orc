include "benchmark.inc"

def doit() =
	val c = Channel[Integer]()
	c.get() + 1331 |
	c.put(1331) >> stop

def run(x) = x >> Let(upto(300) >> doit())

benchmarkSized("Channel2", 1, { signal }, run, { _ = 1331*2 })

{-
OUTPUT:
2662
-}
