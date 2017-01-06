{- trivial_class.orc -- A test of a trivial class and instantiation
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

val DConst =
	val x = Rwait(500) >> 1
	
	class D {
	  val y = x + 1
	}
	def D() = new D
	D


Rwait(250) >> "250ms" |
DConst >> "def" |
DConst().y


{-
OUTPUT:
"250ms"
"def"
2
-}
{-
OUTPUT:
"250ms"
2
"def"
-}