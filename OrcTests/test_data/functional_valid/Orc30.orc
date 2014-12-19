{- Orc30.orc
 - 
 - Testing for Orc guarded clauses
 - in the presence of currying
 - 
 - Created by dkitchin on Oct 14, 2010
 -}

def czad(Integer) :: lambda(Integer) :: Integer
def czad(a) = def f(b) if (a+b = 0) = a+b # f

czad(2)(-2) | czad(3)(4) | czad(0)(0)

{-
OUTPUT:
0
0
-}
