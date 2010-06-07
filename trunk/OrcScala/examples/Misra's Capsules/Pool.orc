{- Pool.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 29, 2010 10:25:00 AM
 -}

{-
A general strategy for allocation, deallocation from a pool.
Argument f to the capsule definition specifies the standard 
allocation function to be used for obtaining an object, as in 
Semaphore(0).

Restrictions:

1. The objects that are allocated have identical initial values. Thus, 
Semaphore(0) and Semaphore(1) can not both be allocated using the 
sempool shown below.

2. The deallocated objects have to have the same initial value. Thus,
every sempahore that is deallocated must have value 0.
 
-}

def capsule Pool(f) = 
 val buff = Buffer()
 
 def allocate()    = buff.getnb() ; f
   
 def deallocate(x) = buff.put(x) 

stop

val sempool = Pool(Semaphore(1))

sempool.allocate() >s> (  
                          s.acquire() >> "got it" 
                        | Rtimer(30) >> s.release() >> "threw it"
                        | Rtimer(50) >> sempool.deallocate(s) >> "gone"
                       )

