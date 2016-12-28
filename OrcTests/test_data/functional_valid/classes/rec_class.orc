{- trivial_class.orc -- A test of a trivial class and instantiation
 -
 - $Id$
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

class C {
  val x = new D
}

class D {
  def f() = new C
  val y = 2
}

(new C).x.f().x.y

{-
OUTPUT:
2
-}