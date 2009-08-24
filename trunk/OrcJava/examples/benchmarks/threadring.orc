{-
Threadring
    * create 503 linked threads (named 1 to 503)
    * thread 503 should be linked to thread 1, forming an unbroken ring
    * pass a token to thread 1
    * pass the token from thread to thread N times
    * print the name of the last thread (1 to 503) to take the token

Description from 
http://shootout.alioth.debian.org/u32q/benchmark.php?test=threadring&lang=all
-}

def threadRing(id, m, in, next) =
    (in.get() >x> if (m = x) then println(id) else next.put(x+1)) >> stop ;threadRing(id, m, in, next)

val N = 503

val ring = IArray(N, lambda(_)=Buffer())

def threadRingRunner(p) = 
upto(N) >i> threadRing(i+1, p, ring(i), ring((i+1) % N)) | ring(0).put(0)


threadRingRunner(1000) >> 
threadRingRunner(10000) >> 
threadRingRunner(100000) >> stop 
{-
OUTPUT:
498
444
407
-}
