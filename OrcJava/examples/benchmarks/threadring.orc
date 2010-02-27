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

def threadRing(Integer, Integer, Buffer[Integer], Buffer[Integer]) :: Integer
def threadRing(id, in, next) =
    in.get() >x> 
    (if (x <= 0) then 
id 
     else 
        next.put(x-1) >> threadRing(id, in, next))

val N = 503

def threadRingRunner(Integer) :: Signal
def threadRingRunner(p) =
  val ring = IArray(N, lambda(_ :: Integer) = Buffer[Integer]()) 
  val _ = ring(0).put(p)
  val lastid = upto(N) >i> threadRing(i+1, ring(i), ring((i+1) % N))
  lastid

def runTest(n, t) =
   val c = Clock()
   c() >start>
   ( val x = threadRingRunner(n) >r> Some(r = n % N + 1) 
  | Rtimer(t) >> None()
     x >> c() >end> (x, end - start)
   )

def metronome2(n, x) = if x > 0 then (signal | Rtimer(n) >> metronome2(n, x-1))
                        else stop      

metronome2(100, 200) >> runTest(1000, 3010)

{-
OUTPUT:
498
444
-}
