{- BarrierSync.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 15, 2010 12:08:26 PM
 -}

{- 
Barrier Synchronization 

n processes do a barrier synchronization by each calling an instance
of BarrierSync(n). Each process puts a signal on buffer in and waits to receive a signal
from buffer out. The manager process collects n signals from in, and
then writes n signals on out.
-}

def class BarrierSync(n) =

  val in =  Semaphore(0) 
  val out = Semaphore(0)  

  def go() = in.release() >> out.acquire()

  {- Repeat f i times -}
  def repeat(0,_) =  signal
  def repeat(i,f) = f() >> repeat(i-1,f)

  def manager() = 
    repeat(n,in.acquire) >> repeat(n,out.release) >> manager()

manager()

     
val barrier = BarrierSync(3).go

  println(0.1) >> barrier() >> println(0.2) >> barrier() >> println(0.3)>> stop 
| println(1.1) >> barrier() >> println(1.2) >> barrier() >> stop 
| println(2.1) >> barrier() >> println(2.2) >> stop
