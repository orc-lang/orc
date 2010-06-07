{- Orc21.orc
 - 
 - Simple test for Orc equality pattern
 - 
 - Created by Brian on Jun 3, 2010 1:41:39 PM
 -}

val x = 7
def isx(=x) = true
def isx(_) = false
isx(7) | isx(9)

{-
OUTPUT:
false
true

OR

OUTPUT:
true
false
-}