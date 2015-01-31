{- barrier-sync.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 15, 2010 12:08:26 PM
 -}

{- 
Barrier Synchronization

n processes do a barrier synchronization by each calling an instance
of BarrierSync(n). Each process puts a signal on channel in and waits to receive a signal
from channel out. The manager process collects n signals from in, and
then writes n signals on out.
-}

class BarrierSync {
  val n :: Integer

  val in =  Semaphore(0)
  val out = Semaphore(0)

  def go() :: Signal = in.release() >> out.acquire()

  {- Repeat f i times -}
  def repeat(Integer, lambda() :: Top) :: Signal
  def repeat(0,_) =  signal
  def repeat(i,f) = f() >> repeat(i-1,f)

  def manager() :: Signal =
    repeat(n,in.acquire) >> repeat(n,out.release) >> manager()

  val _ = manager()
}
def BarrierSync(n' :: Integer) = new BarrierSync with { val n = n' }

val barrier = BarrierSync(3).go

  Println(0.1) >> barrier() >> Println(0.2) >> barrier() >> Println(0.3)>> stop
| Println(1.1) >> barrier() >> Println(1.2) >> barrier() >> stop
| Println(2.1) >> barrier() >> Println(2.2) >> stop
