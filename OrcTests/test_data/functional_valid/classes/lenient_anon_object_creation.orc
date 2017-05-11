{- lenient_object_creation.orc -- Make sure object creation is lenient on the context
 -
 - Created by amp on May 3, 2017
 -}
 
val x = Rwait(500) >> 23

Rwait(250) >> Println("250ms") >> stop
|
(
  val o = new {
    val x' = x
    val y = 1
  }
  o.y
)

{-
OUTPUT:
1
250ms
-}