{- strict-valuation-easy.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that the creation of a closure
 - as a value is strict in the variables in the enclosed
 - function's lexical scope.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rwait(500) >> 23

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
