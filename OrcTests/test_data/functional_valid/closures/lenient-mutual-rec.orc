{- lenient-mutual-rec.orc
 - 
 - A regression test for defined functions.
 -
 - This test verifies that mutually recursive calls within functions 
 - are lenient in the variables of the functions' lexical scope.
 - 
 - Created by dkitchin on Jul 28, 2010
 -}
val x = Rwait(500) >> 23

Rwait(250) >> Println("250ms") >> stop
|
(
  def f(Integer) :: Integer
  def f(0) = Println("f(0)") >> x
  def f(n) = Println("f(" + n + ")") >> Rwait(83) >> g(n-1)
  def g(Integer) :: Integer
  def g(0) = Println("g(0)") >> x
  def g(n) = Println("g(" + n + ")") >> Rwait(83) >> f(n-1)
  f(2) >> stop
) 


{-
OUTPUT:
f(2)
g(1)
f(0)
250ms
-}