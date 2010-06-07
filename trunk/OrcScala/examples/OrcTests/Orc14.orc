{- Orc14.orc
 - 
 - Test for Orc 'fanning out' in sequential operator
 - 
 - Created by Brian on Jun 3, 2010 12:59:34 PM
 -}

def pubNums(n) = if(n > 0) then (n | pubNums(n-1))
pubNums(5) >x> x

{-
OUTPUT:
5
4
3
2
1

OR

any permutation of above
-}