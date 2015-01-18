{- trivial_object.orc -- Orc program trivial_object
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val o = {| new {
  val x = 1
  val y = Rwait(100) >> 2
  def f() = this.y ; 3
  site g() = x
} >o> Rwait(50) >> o |}

o.f() | o.x | o.y | o.g()

{-
OUTPUT:PERMUTABLE:
1
3
-}