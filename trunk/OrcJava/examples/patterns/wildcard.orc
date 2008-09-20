-- Use wildcards to ignore subpatterns
{-
OUTPUT:
4
1
8
5
-}
( (1,(2,3)) | (4,true) | (5,[6,7]) | (8,()) ) >(x,_)> x