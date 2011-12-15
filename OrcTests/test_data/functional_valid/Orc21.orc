{- Orc21.orc
 - 
 - Testing for guarded clauses
 - (equivalent to previous test for 
 -  now-deprecated equality pattern)
 - 
 - Created by dkitchin on Oct 12, 2010
 -}

val x = 7
def isx(Integer) :: Boolean
def isx(y) if (y = x) = true
def isx(_) = false
isx(7) | isx(9)

{-
OUTPUT:PERMUTABLE
false
true
-}
