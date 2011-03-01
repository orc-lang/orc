{- PureRendezvous.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 10, 2010 8:03:02 PM
 -}

def class PureRendezvous() =
val up   = Semaphore(0)
val down = Semaphore(0)

def send() = up.release() >> down.acquire()
def recv() = up.acquire() >> down.release()

stop

val group = PureRendezvous()

 group.send() | Rwait(1000) >> group.recv() 

{-
OUTPUT:
signal
signal
-}
