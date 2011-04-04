{- SemaphoreUsingChannel.orc
 - 
 - $Id$
 - 
 - Created by misra on Apr 15, 2010 2:08:35 PM
 -}

def class NewSemaphore(n) =
 val b = Channel()
 def acquire() = b.get()
 def release() = b.put(signal)

 def add(0) = signal 
 def add(i) = release() >> add(i-1)

add(n)

val s = NewSemaphore(3)

s.acquire() >> s.acquire() >> s.acquire()

{-
OUTPUT:
signal
-}
