{- graft1.orc -- Test graft behaviour of val
 -
 - Created by amp on Nov 28, 2014
 -}

val x = 42 | Rwait(100) >> Println("Later") 
x

{-
OUTPUT:
42
Later
-}