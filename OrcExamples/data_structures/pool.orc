{- pool.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 29, 2010 10:25:00 AM
 -}

{-
A general strategy for allocation, deallocation from a pool.
Argument f to the class definition specifies the standard 
allocation function to be used for obtaining an object, as in 
Semaphore(0).

Restrictions:

1. The objects that are allocated have identical initial values. Thus, 
Semaphore(0) and Semaphore(1) can not both be allocated using the 
sempool shown below.

2. The deallocated objects have to have the same initial value. Thus,
every semaphore that is deallocated must have value 0.
 
-}

type Pool[A] =
  {.
      allocate :: lambda() :: A,
    deallocate :: lambda(A) :: Signal
  .}

def class Pool[A](f :: (lambda() :: A)) :: Pool[A] = 
  val ch = Channel[A]()
  def allocate() = ch.getD() ; f()  
  def deallocate(x :: A) = ch.put(x) 
  stop

val sempool = Pool[Semaphore](lambda () = Semaphore(1))

sempool.allocate() >s> (  
                          s.acquire() >> "got it" 
                        | Rwait(30) >> s.release() >> "threw it"
                        | Rwait(50) >> sempool.deallocate(s) >> "gone"
                       )

{-
OUTPUT:
"got it"
"threw it"
"gone"
-}
