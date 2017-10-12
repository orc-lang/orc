{-
This program creates 2^20 (or about 1 million) threads
and waits for them to terminate.
-}

include "benchmark.inc"

val N = problemSizeLogScaledInt(18)
def threads(n) = if n/=0 then n-1 >n'> (threads(n') | threads(n')) else stop

benchmarkSized("Threads", N, { signal }, { threads(N) ; signal })

{-
BENCHMARK
-}
