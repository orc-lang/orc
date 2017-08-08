{- site9.orc -- Test site running in a group and being called from outside.
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

val test = Cell() #

((
site Test(x :: Integer) :: Integer = x+1 | Rwait(101) >> 9 

test := Test >> stop) ; "Error!!") | 

test?(10)

{-
OUTPUT:
11
9
-}
