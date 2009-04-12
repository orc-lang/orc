-- Filtration based on list structure
{-
OUTPUT:
(4, 5, [])
(1, 2, [3])
-}
( [1,2,3] | [4,5] | [6] | [] ) >a:b:c> (a,b,c)
