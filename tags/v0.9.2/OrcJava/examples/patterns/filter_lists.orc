-- Filtration based on list structure
-- Output: (1,2,[3]) and (4,5,[])

( [1,2,3] | [4,5] | [6] | [] ) >a:b:c> (a,b,c)
