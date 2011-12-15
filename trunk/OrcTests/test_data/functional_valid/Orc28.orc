{- Orc28.orc
 - 
 - Testing for Orc guarded clauses
 - 
 - Created by dkitchin on Oct 12, 2010
 -}

def fact(Integer) :: Integer
def fact(n) if (n :> 0) = n * fact(n-1)
def fact(0) = 1

fact(-1) ; fact(0) | fact(4)

{-
OUTPUT:PERMUTABLE:
1
24
-}
