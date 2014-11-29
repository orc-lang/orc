{- hrqs.orc -- Orc program Highly Recursive Quick Sort
 - 
 - Created by Amin Shali on Ausg 23, 2010

The idea of this code is from
http://www.cs.cmu.edu/~scandal/nesl/alg-sequence.html
 -}

def qs[A](List[A]) :: List[A]
def qs(a) =
  val pivot = index(a, length(a)/2)
  val less = filter(lambda(x :: A) = x <: pivot, a)
  val equal = filter(lambda(x :: A) = x = pivot, a)
  val greater = filter(lambda(x :: A) = x :> pivot, a)
  if length(a) :> 1 then (append(append(qs(less), equal), qs(greater)))
  else a

def makeNRandomInt(Integer) :: List[Integer]
def makeNRandomInt(0) = []
def makeNRandomInt(n) = Random(100):makeNRandomInt(n-1)

val l = makeNRandomInt(100)
val (x, y) = (qs(l), sort(l)) 
x = y 

{-
OUTPUT:
true
-}
