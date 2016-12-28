{- trivial_class.orc -- A test of a trivial class and instantiation
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val x = 1

class D {
  val y = x + 1
}
def D() = new D

D().y

{-
OUTPUT:
2
-}