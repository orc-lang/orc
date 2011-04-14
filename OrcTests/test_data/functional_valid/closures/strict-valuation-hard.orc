{- strict-valuation-hard.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that the creation of a closure
 - as a value is strict in the variables in the enclosed
 - function's lexical scope.
 - 
 - This is a more subtle version of the test which
 - requires very strict adherence to the Orc semantics;
 - the creation of the closure must succeed even if
 - some of its free variables have halted and thus will
 - never be bound.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rwait(500) >> stop

Rwait(250) >> Println("250ms") >> stop
|
(
  def f() = x
  f >> Println("closure of f created") >> stop
)

{-
OUTPUT:
250ms
closure of f created
-}