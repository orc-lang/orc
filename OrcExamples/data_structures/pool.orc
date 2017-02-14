{- pool.orc
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

-- TODO: TYPING: Add type member for resource type.
class Pool {
  val f
  
  val ch = Channel()
  def allocate() = ch.getD() ; f()
  def deallocate(x) = ch.put(x)
}
def Pool[A](f_ :: (lambda() :: A)) :: Pool[A] =
  new Pool { val f = f_ }

val sempool = Pool[Semaphore]({ Semaphore(1) })

sempool.allocate() >s> (
                          s.acquire() >> "got it"
                        | Rwait(50) >> s.release() >> "threw it"
                        | Rwait(100) >> sempool.deallocate(s) >> "gone"
                       )

{-
OUTPUT:
"got it"
"threw it"
"gone"
-}
