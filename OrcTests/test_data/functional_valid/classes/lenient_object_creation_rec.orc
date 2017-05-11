{- lenient_object_creation.orc -- Make sure object creation is lenient on the context even when the class is recursive with a def
 - However the def should be strict.
 -
 - Created by amp on May 3, 2017
 -}
 
def id(x) = x
 
val x = Rwait(500) >> 23

class C {
  val x' = id(x)
  val y = 1
  def other() = C()
}
def C() = x >> new C

Rwait(250) >> Println("250ms") >> stop
|
(
  val o = new C
  val o' = o.other()
  o.y | (o'.y + 1)
)

{-
OUTPUT:
1
250ms
2
-}