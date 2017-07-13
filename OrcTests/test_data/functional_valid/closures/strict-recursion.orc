{- lenient-recursion.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that recursive calls within a function
 - are lenient in the variables of the function's lexical scope.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rwait(500) >> 23

Rwait(250) >> Println("250ms") >> stop
|
(
  def f(Integer) :: Integer
  def f(0) = Println("f(0)") >> x
  def f(n) = Println("f(" + n + ")") >> Rwait(83) >> f(n-1)
  f(2) >> stop
)


{-
OUTPUT:
250ms
f(2)
f(1)
f(0)
-}
