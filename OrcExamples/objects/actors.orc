{- actors.orc -- Building an actor using Orc Objects
 -
 - Created by amp on Feb 20, 2015 1:06:57 PM
 -}
 
include "actor.inc"

-- A simple actor that merges two messages and prints the result.
class Actor extends ActorBase {
  val target :: ActorBase

  val _ = repeat({ -- Body
    receive({ _ >("test", v)> v }) >v>
    receive({ _ >("other", v')> Println("Other: " + v + " " + v') >> target.sendMessage(("pair", v, v')) })
  })
}

class SumOfProducts extends ActorBase {
  val sum = Ref(0)

  val _ = repeat({ -- Body
    receive(lambda(m) =
    	      m >("pair", x, y)> sum := sum? + x * y |
    	      m >("print")> Println("Result: " + sum?))
  })
}

{|
val sum = new SumOfProducts
val a = new Actor { val target = sum }  #

(
a.sendMessage(("other", 2)) |
a.sendMessage(("test", 1)) |
Rwait(100) >> a.sendMessage(("test", 10)) |
Rwait(700) >> a.sendMessage(("other", 3)) |
Rwait(500) >> sum.sendMessage("print")
) >> stop |
Rwait(1000) >> sum.sendMessage("print") >> Rwait(100)
|}

{-
OUTPUT:
Other: 1 2
Result: 2
Other: 10 3
Result: 32
signal
-}
