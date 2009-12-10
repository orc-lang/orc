-- Otherwise.orc -- Demonstrate the otherwise combinator in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

low ; high

{-
TYPE:  Integer{6}
OUTPUT:
1
-}
