{- Orc23.orc
 - 
 - Simple test for Orc classs
 - 
 - Created by Brian on Jun 3, 2010 1:46:23 PM
 -}

def class A() =
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