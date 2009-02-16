{--
Write a program with three functions: inc, dec, and read.
Each of these functions modifies a shared state which is
a natural number (n), initially = 0.  When inc is called,
increment n.  When dec is called, decrement n.  When read
is called, publish the value of n.  Each of these
functions must act atomically, so that the expression
<code>inc() | dec()</code> is guaranteed to leave the
counter state unchanged after it completes.

Hint: use a semaphore to control access to the shared state.
--}

val n = Ref(0)
val lock = Semaphore(1)

def inc() =
  lock.acquire() >>
  n := n? + 1 >>
  lock.release()

def dec() =
  lock.acquire() >>
  n := n? - 1 >>
  lock.release()

def read() =
  lock.acquire() >>
  n? >out>
  lock.release() >>
  out

inc() >> signals(10) >> (inc(), dec()) >> stop
; read()

{-
OUTPUT:
1
-}