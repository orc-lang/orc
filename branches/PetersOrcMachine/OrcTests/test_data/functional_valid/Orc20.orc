{- Orc20.orc
 - 
 - Test for Orc currying
 - 
 - Created by Brian on Jun 3, 2010 1:39:31 PM
 -}

def Sum(Integer) :: lambda (Integer) :: Integer
def Sum(a) = ( lambda(b) = a+b )
val f = Sum(3)
f(4)

{-
OUTPUT:
7
-}
