{- Orc15.orc
 - 
 - Test for Orc timeout idiom
 - 
 - Created by Brian on Jun 3, 2010 1:03:33 PM
 -}

let(stop >x> let(x, true) | Rtimer(2000) >y> let(y, false))

{-
OUTPUT:
(signal, false)
-}