{- Orc19.orc
 - 
 - Test for Orc dot operator
 - 
 - Created by Brian on Jun 3, 2010 1:35:00 PM
 -}

val a = Channel[Integer]()
a.put(8) >> a.get()

{-
OUTPUT:
8
-}
