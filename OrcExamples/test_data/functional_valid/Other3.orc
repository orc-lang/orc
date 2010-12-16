{- Other3.orc
 - 
 - Tests the new "if" true site
 - 
 - Created by brian on Jun 8, 2010 4:01:05 PM
 -}

val b = true

If(b) >> "true" | Unless(b) >> "false"

{-
OUTPUT:
"true"
-}