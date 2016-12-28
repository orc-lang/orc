{- trivial_object.orc -- Orc program trivial_object
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = new {
  val x = 1
  val y = this.x
} #
(o.x + 10) | o.y

{-
OUTPUT:PERMUTABLE:
11
1
-}