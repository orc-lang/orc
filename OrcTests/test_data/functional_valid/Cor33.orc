{- Cor33.orc
 - 
 - Simple test of Cor clausal definitions
 - 
 - Created by Brian on Jun 3, 2010 10:08:24 AM
 -}

def pow2(Integer) :: Integer
def pow2(0) = 1
def pow2(n) = 2 * pow2(n-1)
pow2(8)

{-
OUTPUT:
256
-}