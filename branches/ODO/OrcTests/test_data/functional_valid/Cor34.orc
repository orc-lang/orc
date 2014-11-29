{- Cor34.orc
 -
 - Simple test of Cor mutual recursion
 -
 - Created by Brian on Jun 3, 2010 10:11:04 AM
 -}

def even(Integer) :: Boolean
def odd(Integer) :: Boolean
def even(0) = true
def odd(0) = false
def even(n) = (if n :> 0 then n-1 else n+1) >i> odd(i)
def odd(n) = (if n :> 0 then n-1 else n+1) >i> even(i)

even(9)

{-
OUTPUT:
false
-}
