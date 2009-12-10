-- Sequential.orc -- Demonstrate the sequential combinator in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

high >x> low

{-
TYPE:  Integer{6}
OUTPUT:
1
-}
