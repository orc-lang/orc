{- site8.orc -- Test that site publications are dropped if caller is killed
 -
 - Created by amp on July 13, 2017
 -}

site Test(x :: Integer) :: Integer = x+1 | Rwait(100) >> x #
 
{| Test(10) |} >x> Rwait(200) >> x

{-
OUTPUT:
11
-}
