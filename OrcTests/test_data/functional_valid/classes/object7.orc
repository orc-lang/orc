{- trivial_object.orc -- Orc program trivial_object
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = new {
  thiso #
  val x = 1
  val y = thiso.x + this.x
} #
(o.x + 10) | o.y

{-
OUTPUT:PERMUTABLE:
11
2
-}
