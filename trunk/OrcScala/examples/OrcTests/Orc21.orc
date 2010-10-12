{- Orc21.orc
 - 
 - Testing for guarded clauses
 - (equivalent to previous test for 
 -  now-deprecated equality pattern)
 - 
 - Created by Brian on Jun 3, 2010 1:41:39 PM
 -}

val x = 7
def isx(y) when (y = x) = true
def isx(_) = false
isx(7) | isx(9)

{-
OUTPUT:PERMUTABLE
false
true
-}