{- lenient-call.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that a call to a function is lenient
 - in the variables of the function's lexical scope.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rwait(500) >> 23

Rwait(250) >> Println("250ms") >> stop
|
(
  def f() = Println("f()") >> x
  f() >> stop
)

{-
OUTPUT:
f()
250ms
-}
