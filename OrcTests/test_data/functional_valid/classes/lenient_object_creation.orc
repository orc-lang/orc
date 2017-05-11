{- lenient_object_creation.orc -- Make sure object creation is lenient on the context
 -
 - Created by amp on May 3, 2017
 -}
 
def id(x) = x
 
val x = Rwait(500) >> 23

class C {
  val x' = id(x)
  val y = 1
}

Rwait(250) >> Println("250ms") >> stop
|
(
  val o = new C
  o.y
)

{-
OUTPUT:
1
250ms
-}