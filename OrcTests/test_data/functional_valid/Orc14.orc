{- Orc14.orc
 -
 - Test for Orc 'fanning out' in sequential operator
 -
 - Created by Brian on Jun 3, 2010 12:59:34 PM
 -}

def pubNums(Integer) :: Integer
def pubNums(n) = if(n :> 0) then (n | pubNums(n-1)) else stop
pubNums(5) >x> x

{-
OUTPUT:PERMUTABLE:
5
4
3
2
1
-}
