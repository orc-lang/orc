{- Cor38.orc
 -
 - Test for Cor record extension
 -
 - Created by dkitchin on Jul 8, 2010 11:44:00 PM
 -}

val a = {. x = 0 .} + {.  .}
val b = {.  .} + {. y = 1 .}
val c = {. x = 10 .} + {. x = 2 .}
val d = {. x = 11, y = 4 .} + {. z = 5, x = 3 .} 
val e = {. x = {. y = 12 .}, z = 13 .} + {. x = {. y = 6, z = 7 .}, y = 8, z = 9 .} 

a.x | b.y | c.x | d.x | d.y | d.z | e.x.y | e.x.z | e.y | e.z

{-
OUTPUT:PERMUTABLE:
0
1
2
3
4
5
6
7
8
9
-}
