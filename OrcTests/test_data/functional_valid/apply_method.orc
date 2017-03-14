{- apply_method.orc -- Test apply methods making things callable.
 -
 - Created by amp on Mar 12, 2017 12:47:33 PM
 -}

val o = new {
  def apply() = Println("Test1")
}

val r = {.
  apply = lambda() = Println("Test2")
.}

o() >> r() >> stop

{-
OUTPUT:
Test1
Test2
-}