{- trivial_class.orc -- A test of a trivial class and instantiation
 -
 - $Id: trivial_class.orc 3387 2015-01-12 21:57:11Z arthur.peters@gmail.com $
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

(new C).x.f().y

{-
Somewhere I'm building the wrong stack. Probably in conversion to nameless. I suspect class values are being left out in some case or another.

The above program should be giving a non-existant field error.
-}

{-
OUTPUT:
2
-}