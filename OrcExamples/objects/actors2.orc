{- actors.orc -- Building an actor using Orc Objects
 -
 - Created by amp on Feb 20, 2015 1:06:57 PM
 -}
 
include "actor.inc"

class Actor extends ActorBase {
  val a = Ref[Integer](0)
  val b = Ref[Integer](0)
  val c = Ref[Integer](0)

  val _ = repeat({
    receive(lambda(m) =
    	      m >("incr")> (a := a? + 1, b := b? + 1, c := c? + 1) |
    	      m >("read", other)> other.sendMessage((a?, b?, c?)))
  })
}

{|

val flag = Cell()

new ActorBase {
  val count = Ref(0)
  val _ = (
    val o = new Actor #
    (upto(40) >> o.sendMessage("incr") | upto(40) >> o.sendMessage(("read", this))) >> stop ;
    Println("Reading") >>
    repeat({
      receive({ _ >(a, b, c) as t> (if a = b && b = c then count := count? + 1 else Println("Fail " + t)) }) >>
      (if count? >= 40 then Println("success") >> flag := signal else signal)
    })
  )
} >> stop
|
flag?

|}

{-
OUTPUT:
Reading
success
signal
-}
