-- Pattern matching on a list
{-
OUTPUT:
([4], 3, 2, 1)
-}
[1,2,3,4] >a:t> t >b:c:u> (u,c,b,a)
