-- Using 'as' to capture sub-patterns
{-
OUTPUT:
(1, 2, 3, (2, 3))
(4, 5, 6, (5, 6))
-}
( (1,(2,3)) | (4,(5,6)) ) >(a,(b,c) as d)> (a,b,c,d)
