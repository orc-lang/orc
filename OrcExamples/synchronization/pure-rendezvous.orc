{- pure-rendezvous.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 10, 2010 8:03:02 PM
 -}

class PureRendezvous {
	val up   = Semaphore(0)
	val down = Semaphore(0)
	
	def send() = up.release() >> down.acquire()
	def recv() = up.acquire() >> down.release()
}

val group = new PureRendezvous

group.send() | Rwait(1000) >> group.recv()

{-
OUTPUT:
signal
signal
-}
