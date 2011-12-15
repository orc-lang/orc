{- Orc15.orc
 - 
 - Test for Orc timeout idiom
 - 
 - Created by Brian on Jun 3, 2010 1:03:33 PM
 -}

Let(stop >x> Let(x, true) | Rwait(2000) >y> Let(y, false))

{-
OUTPUT:
(signal, false)
-}
