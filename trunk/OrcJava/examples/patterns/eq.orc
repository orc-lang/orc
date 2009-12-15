-- Equality pattern

val y = 1
( (1,4) | (2,5) | (1,6) )  >(=y,x)> x 

{-
OUTPUT:
4
6
-}
