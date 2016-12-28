{- Cor32.orc
 - 
 - Simple test for Cor lambda closures
 - 
 - Created by Brian on Jun 3, 2010 10:06:06 AM
 -}

def onetwosum(lambda (Integer) :: Integer) :: Integer
def onetwosum(f) = f(1) + f(2)
onetwosum( { _ * 3 } )

{-
OUTPUT:
9
-}
