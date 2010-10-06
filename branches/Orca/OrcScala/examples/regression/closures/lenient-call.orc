{- lenient-call.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that a call to a function is lenient
 - in the variables of the function's lexical scope.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rtimer(500) >> 23

Rtimer(250) >> println("250ms") >> stop
|
(
  def f() = println("f()") >> x
  f() >> stop
) 

{-
OUTPUT:
f()
250ms
-}