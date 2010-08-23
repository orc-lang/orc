{-
Highly Recursive Quick Sort
author: Amin Shali
date: Monday, August 23 2010
The idea of this code is from 
http://www.cs.cmu.edu/~scandal/nesl/alg-sequence.html 
-}

def qs(a) =
 val pivot = index(a, length(a)/2)
 val less = filter(lambda(x) = x <: pivot, a)
 val equal = filter(lambda(x) = x = pivot, a)
 val greater = filter(lambda(x) = x :> pivot, a)
 if length(a) :> 1 then append(append(qs(less), equal), qs(greater))
 else a

class Random = java.util.Random
val random = Random()
def makeNRandomInt(0) = []
def makeNRandomInt(n) = random.nextInt(100):makeNRandomInt(n-1)

x = y <(x, y)< (qs(l), sort(l)) <l< makeNRandomInt(100)

{-
OUTPUT:
true
-}

