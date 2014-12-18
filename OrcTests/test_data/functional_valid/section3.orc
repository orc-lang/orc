{- section1.orc -- Orc program section1
 -
 - $Id$
 -
 - Created by amp on Dec 17, 2014 2:38:20 PM
 -}

val data = [[1,2,3],[4,5]]
map({ map({ (_ :: Integer) + 10 }, _ :: List[Integer]) }, data)

{-
OUTPUT:
[[11, 12, 13],[14, 15]]
-}