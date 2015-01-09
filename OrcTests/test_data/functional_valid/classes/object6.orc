{- trivial_object.orc -- Orc program trivial_object
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

{|
val o = new {
  val x = 1
  val _ = repeat({ this })
}
o.x
|}

{-
OUTPUT:
1
-}