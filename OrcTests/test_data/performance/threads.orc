{-
This program creates 2^20 (or about 1 million) threads
and waits for them to terminate.
-}

include "benchmark.inc"

import class Threads = "orc.test.item.scalabenchmarks.Threads"

val N = Threads.N()
def threads(n) = 
	if n/=0 then 
		n-1 >n'> (threads(n') | threads(n')) 
	else 
		stop

benchmarkSized("Threads", 2 ** N, { signal }, { _ >> threads(N) ; signal }, { _ = signal })

{-
BENCHMARK
-}
