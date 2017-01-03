{- trivial_object.orc -- Orc program trivial_object
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = new {
  val (x, y) = (1, 2)
}

o.x | o.y

{-
OUTPUT:PERMUTABLE:
1
2
-}