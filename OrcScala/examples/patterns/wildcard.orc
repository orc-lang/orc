-- Use wildcards to ignore subpatterns
{-
OUTPUT:PERMUTABLE
1
4
5
8
-}
( (1,(2,3)) | (4,true) | (5,[6,7]) | (8,signal) ) >(x,_)> x