{- site2.orc -- Test site not running in the call termination domain
 -
 - $Id$
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

site Test(x :: Integer) :: Integer = x+1 | Rwait(101) >> Println(x) >> 9

{| Test(10) |}

{-
OUTPUT:
11
10
-}