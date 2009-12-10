-- Conditional parallel.orc -- Demonstrate another control flow dependency in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

if(high > 2) | signal

{-
TYPE:  Signal{6}
OUTPUT:
signal
signal
-}
