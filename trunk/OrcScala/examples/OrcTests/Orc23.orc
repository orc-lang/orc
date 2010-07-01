{- Orc23.orc
 - 
 - Simple test for Orc capsules
 - 
 - Created by Brian on Jun 3, 2010 1:46:23 PM
 -}

def capsule A() =
	val x = 4
	def myX() = 
		x
	signal

val a = A()

a.myX()

{-
OUTPUT:
4
-}