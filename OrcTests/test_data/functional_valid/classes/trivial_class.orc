{- trivial_class.orc -- A test of a trivial class and instantiation
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

class C {
  val x = 1
}

(new C).x

{-
OUTPUT:
1
-}