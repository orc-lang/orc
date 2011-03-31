{- Cor36.orc
 -
 - Simple test for Cor recursion
 -
 - Created by Brian on Jun 3, 2010 10:14:42 AM
 -}

def sumto(Integer) :: Integer
def sumto(n) = if n <: 1 then 0 else n + sumto(n-1)
sumto(10)

{-
OUTPUT:
55
-}