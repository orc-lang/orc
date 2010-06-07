{- Orc13.orc
 - 
 - Simple test of Orc let() site
 - 
 - Created by Brian on Jun 3, 2010 12:55:42 PM
 -}

let(2, 3, 4) >(x,y,z)> let (x | z)

{-
OUTPUT:
4

OR

OUTPUT:
2
-}