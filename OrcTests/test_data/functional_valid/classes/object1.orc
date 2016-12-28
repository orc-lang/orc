{- trivial_object.orc -- Orc program trivial_object
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = new {
  val x = 1
  def f() = this.x
}
o.f()

{-
OUTPUT:
1
-}