{- lenient-def.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that the body of a function declaration
 - can proceed even if some variables in the function body
 - are free.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rtimer(500) >> 23

Rtimer(250) >> println("250ms") >> stop
|
(
  def f() = x
  println("scope of f") >> f() >> stop
) 

{-
OUTPUT:
scope of f
250ms
-}
