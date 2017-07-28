{- sequential_objects.orc -- Using Orc Objects as normal sequential objects
 -
 - Created by amp on Feb 20, 2015 3:55:13 PM
 -}

class Unsafe {
  val a = Ref[Integer](0)
  val b = Ref[Integer](0)
  val c = Ref[Integer](0)

  def read() = (a?, b?, c?)
  
  def incr() = (a := a? + 1, b := b? + 1, c := c? + 1) >> signal
}

class Safe extends Unsafe {
  val lock = Semaphore(1)

  def read() = withLock(lock, super.read)
  
  def incr() = withLock(lock, super.incr)
}

(
val o = new Safe

upto(1000) >> o.incr() >> stop |
upto(1000) >> o.read() >(a, b, c) as t> (if a = b && b = c then stop else "Fail " + t)
)
|
{|
val o = new Unsafe

upto(1000) >> o.incr() >> stop |
upto(1000) >> o.read() >(a, b, c) as t> (if a = b && b = c then stop else "Fail " + t)
|} >> "Unsafe failed"

{-
OUTPUT:
"Unsafe failed"
-}
