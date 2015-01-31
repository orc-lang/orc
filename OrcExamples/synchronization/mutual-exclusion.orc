{- mutual-exclusion.orc
 - 
 - $Id$
 - 
 - Created by misra on Apr 15, 2010 2:13:49 PM
 -}

{- 
Suppose many processes call a server simultaneously, but only one copy
of the server should execute at any time. We can ask the processes to
acquire a lock before calling the server and release it afterwards. But
this exposes the lock and introduces the possibility of some one
process making an error.

Similarly, the lock can be accessed by the server, but it still exposes the lock.
We would like to encapsulate the lock.
-}

{- Typical call
-}
def Server[A](j :: A) = j

class Servercall {
  type A
  val lock = Semaphore(1)
  def main(v :: A) =
    lock.acquire() >> Server(v) >w> lock.release() >> w
}
def Servercall[A']() = new Servercall with { type A = A' }


val serve = Servercall[Integer]().main

serve(5)

{-
OUTPUT:
5
-}
