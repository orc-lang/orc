{- trivial_object.orc -- Orc program trivial_object
 -
 - Created by amp on Jul 28, 2017
 -}

val o = new {
  val x = 1
}
val r = Ref(o)
r.read().x

{-
OUTPUT:
1
-}
