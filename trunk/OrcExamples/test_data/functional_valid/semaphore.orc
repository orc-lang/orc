val s = Semaphore(1)

signals(50) >>
s.acquire() >>
Println("Entering critical section") >>
Rtimer(1) >>
Println("Leaving critical section") >>
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
