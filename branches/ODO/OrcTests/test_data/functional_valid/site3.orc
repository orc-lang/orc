{- site3.orc -- Test site multipe publication
 -
 - $Id$
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

val Passed = (site Test(x) = x+1 | x # Test)
Passed(10)

{-
OUTPUT:PERMUTABLE:
11
10
-}