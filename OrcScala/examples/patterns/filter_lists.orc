-- Filtration based on list structure

( [1,2,3] | [4,5] | [6] | [] ) >a:b:c> (a,b,c)

{-
OUTPUT:
(1, 2, [3])
(4, 5, [])
-}
{-
OUTPUT:
(4, 5, [])
(1, 2, [3])
-}