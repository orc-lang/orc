{- trivial_object.orc -- Orc program trivial_object
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = new { val x = 1 }
o | o.x


{-
OUTPUT:PERMUTABLE:
{ .x = BoundValue(1) }
1
-}