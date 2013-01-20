{- sequencer.orc
 - 
 - Created by misra on Mar 25, 2010 4:16:29 PM
-}

{- 
Sequencer simulates ordering of a set of requesters of a service
(customer).  It has two methods: incr() and register().

A customer requests service by calling register(); this call responds
only when he is granted service.

The server(s) call incr() when they are ready to serve the next
customer. Each call to incr() triggers the earliest remaining 
customer to receive service, i.e., his call to register() 
to respond. If there are k servers there may be k simultaneous 
executions of incr(). With k = 1, we have mutual execution in service.

The parameter n is the maximum number of customers that may be in 
the queue waiting to be served. 

The implementation uses the call-back mechanism used in the 
Readers-Writers solution.
-}
include "../synchronization/BoundedChannel.inc"
include "../clock_time/Stopwatch.inc"

def class Sequencer(n :: Integer) = 
 val bb = BChannel[Semaphore](n)  -- waiting customers' semaphores
 val sem = BChannel[Semaphore](n) -- semaphore pool
 
 def  incr() =  bb.get() >s> s.release() 

 def register() = 
      sem.get() >s> bb.put(s) >> s.acquire() >> sem.put(s)

  {- allocate n semaphores in the pool at start. -}
  upto(n) >> 
  (val s = Semaphore(0)
   sem.put(s))

val cc = Sequencer(2)
val sw1 = Stopwatch()
val sw2 = Stopwatch()
  sw1.start() >> cc.register() >> sw1.pause() >t> Rwait(50) >>  (1,t)
| sw2.start() >> Rwait(10) >> cc.register() >> sw2.pause() >t> (2,t)
| cc.incr() >> stop | Rwait(80) >> cc.incr() >> stop
