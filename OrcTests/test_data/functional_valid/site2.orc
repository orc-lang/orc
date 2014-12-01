{- site2.orc -- Test site not running in the call termination domain
 -
 - $Id$
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

site Test(x) = x+1 | Rwait(100) >> Println(x)

{| Test(10) |}

{-
OUTPUT:
11
10
-}