-- Sequential.orc -- Demonstrate the sequential combinator in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{A4}
val high = 6 :: Integer{A6}
val unused = 9 :: Integer{F9}

high >x> low

{-
TYPE:  Integer{A6}
OUTPUT:
1
-}
