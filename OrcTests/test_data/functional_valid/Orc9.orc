{- Orc9.orc
 - 
 - Simple test for Orc if-then-else equivalency
 - 
 - Created by Brian on Jun 3, 2010 11:07:52 AM
 -}

val b = true
Ift(b) >> 1 | Ift(~b) >> 0

{-
OUTPUT:
1
-}
