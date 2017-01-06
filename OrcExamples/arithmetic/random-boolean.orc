{- random-boolean.orc
 - 
 - Created by misra on Mar 13, 2010 3:43:38 PM
 -}

{- RandomBool() has:
   f a real number between 0 and 1,
   t an integer,
   A call to site RandomBool.main(f,t) responds with
     probability f
     after some random time at most t
     with a random boolean.
   (Note that the call just halts silently with probability 1-f.)

  It has just one method, main(). It handles one call at a time.
-}

class def RandomBool() :: RandomBool {
  val s = Semaphore(1) -- to allow only one call to execute.
  def main(Number, Integer) :: (Integer, Boolean)
  def main(f,t) =
    Ift(URandom() <: f) >>  this.s.acquire() >>
    Random(t) >w> Rwait(w) >>
    (Random(2) = 1) >v>
    this.s.release() >>
    (w,v)
}

val rb = RandomBool().main
val (_,x) = rb(0.5,3000)
val (_,y) = rb(1,3000)
val z = Ift(x) >> true | Ift(y) >> true | x||y

z
