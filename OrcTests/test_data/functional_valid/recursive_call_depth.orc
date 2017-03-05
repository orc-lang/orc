{- recursive_call_depth.orc -- Orc program to test that a reasonable recursion depth works
 -
 - Created by amp on Mar 4, 2017 11:47:17 AM
 -}

def rangeBy(Number, Number, Number) :: List[Number]
def rangeBy(low, high, skip) =
  if low <: high
  then low:rangeBy(low+skip, high, skip)
  else []

-- val N = 9921 -- OpenJDK succeeds (fails on 9922)
-- val N = 9911 -- Oracle succeeds (fails on 9912)
val N = 8000

upto(100) >>
rangeBy(0, N, 1) >> stop ;
"Done"

{-
OUTPUT:
"Done"
-}