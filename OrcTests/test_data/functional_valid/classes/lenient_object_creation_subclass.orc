{- lenient_object_creation_subclass.orc -- Make sure object creation is lenient on the context
 -
 - Created by amp on May 3, 2017
 -}
 
def id(x) = x
 
val x = Rwait(500) >> 23

class C {
  val x' = id(x)
}

Rwait(250) >> Println("250ms") >> stop
|
(
  val o = new C {
	val y = 1
  }
  o.y
)

{-
OUTPUT:
1
250ms
-}