{- Orc9.orc
 - 
 - Simple test for Orc if-then-else equivalency
 - 
 - Created by Brian on Jun 3, 2010 11:07:52 AM
 -}

(If(b) >> 1 | If(~b) >> 0) <b< true

{-
OUTPUT:
1
-}
