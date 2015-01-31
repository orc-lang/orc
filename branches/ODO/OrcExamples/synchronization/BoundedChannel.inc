{- BoundedChannel.inc
 - 
 - $Id$
 - 
 - Created by misra on Mar 11, 2010 8:57:43 PM
 -}

{- 
A bounded channel of size n, n > 0, is defined by an array of size n,
n > 0, its rear r and the front f (both Refs). Access to f and r are
guarded by fs and rs.

Below, puts is a semaphore whose value is the number of empty
positions in the channel, gets is a semaphore whose value is the
number of full positions. A put operation is started only if there is
some empty position, then it gains access to r using the
semaphore rs.  Function gets is the dual of puts.

We could have a single semaphore bs instead of fs and rs.
   That will allow at most one put and get operation to operate
   at any moment. The current implementation allows one instance of
   put and another instance of get to operate simultaneously, because
   they never access the same location in the array (if both put and get
   are operating, then there is some full positions and some empty
   positions; therefore f /= r).

-}

{- Orc standard library provides Bounded Channel -}
class BChannel {
  -- type A
  -- TODO: Use abstract type members when they are available
  val n :: Integer
  
  val B = Array(n)
  val f = Ref[Integer](0)
  val fs = Semaphore(1)   -- to guard f
  val r = Ref[Integer](0)
  val rs = Semaphore(1)   -- to guard r
  val puts = Semaphore(n) -- # of empty positions
  val gets = Semaphore(0) -- # of full positions

  def put(x) =
    puts.acquire() >> rs.acquire() >> B(r?) := x >>
    (r := (r? +1) % n) >> rs.release() >> gets.release()

  def get() =
    gets.acquire() >> fs.acquire() >> B(f?)? >v>
    (f := (f? +1) % n) >> fs.release() >> puts.release() >> v
}
def BChannel[A'](n' :: Integer) = new BChannel with { 
					val n = n' 
					-- type A = A'
				      }
-- TODO: Convert this to use constructor syntactic sugar when it is available.

{- Word.

   This is derived from Bounded Channel. Since there is just one word,
   both f and r are permanently 1. Further, since n = 1, only one
   instance of put or get can execute at any moment. Therfore, we may
   dispense with fs and rs.
-}
class Word {
  -- type A
  -- TODO: Use abstract type members when they are available

  val w = Ref()
  val puts = Semaphore(1) -- # of empty positions
  val gets = Semaphore(0) -- # of full positions

  def put(x) =
    puts.acquire() >> w := x >> gets.release()

  def get() =
    gets.acquire()  >> w? >v> puts.release() >> v
}
def Word[A']() = new Word -- with { type A = A' }
-- TODO: Convert this to use constructor syntactic sugar when it is available.
