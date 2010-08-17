{- Other3.orc
 - 
 - Tests the new "if" true site
 - 
 - Created by brian on Jun 8, 2010 4:01:05 PM
 -}

val b = true

IfT(b) >> "true" | IfF(b) >> "false"

{-
OUTPUT:
"true"
-}