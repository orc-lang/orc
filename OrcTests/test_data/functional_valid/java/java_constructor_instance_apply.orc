{- scala_constructor.orc -- Test constructing objects which have an apply instance method.
 - 
 - Created by amp on Jan 16, 2017
 -}

import class Set = "scala.collection.mutable.HashSet"

Set() >> "PASS"

{-
OUTPUT:
"PASS"
-}