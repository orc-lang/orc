-- Equality pattern

val y = 1 within
( (1,4) | (2,5) | (1,6) )  >(=y,x)> x 

{-
OUTPUT:PERMUTABLE
4
6
-}