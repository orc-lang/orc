val s = Semaphore(1)

signals(50) >>
s.acquire() >>
println("Entering critical section") >>
Rtimer(1) >>
println("Leaving critical section") >>
s.release() >> stop

{-
OUTPUT:
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
Entering critical section
Leaving critical section
-}
