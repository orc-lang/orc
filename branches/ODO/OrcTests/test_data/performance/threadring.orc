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

def threadRing(Integer, Integer, Channel[Integer], Channel[Integer]) :: Integer
def threadRing(id, m, in, next) =
    in.get() >x>
    (if (m = x) then
  id
     else
        next.put(x+1) >> threadRing(id, m, in, next))

val N = 503

def threadRingRunner(Integer) :: Signal
def threadRingRunner(p) =
  val ring = Table(N, lambda(_ :: Integer) = Channel[Integer]())
  val _ = ring(0).put(0)
  val lastid = {| upto(N) >i> threadRing(i+1, p, ring(i), ring((i+1) % N)) |}
  Println(lastid)

threadRingRunner(1000) >>
threadRingRunner(10000) >> stop
{-
OUTPUT:
498
444
-}
