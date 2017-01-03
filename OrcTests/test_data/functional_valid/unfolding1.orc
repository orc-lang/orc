{- unfolding1.orc -- Test graft behaviour of unfolding
 -
 - Created by amp on Nov 28, 2014
 -}

Let(42 | Rwait(100) >> Println("Later"))

{-
OUTPUT:
42
Later
-}