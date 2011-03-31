{- Orc29.orc
 - 
 - Testing for Orc guarded clauses
 - 
 - Created by dkitchin on Oct 12, 2010
 -}

def fib(Integer) :: Integer
def fib(0) = 0
def fib(1) = 1
def fib(n) if (n :> 1) = fib(n-1) + fib(n-2)

fib(-1) ; fib(3)

{-
OUTPUT:
2
-}