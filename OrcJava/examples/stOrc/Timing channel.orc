-- Timing channel.orc -- Demonstrate a timing channel leak
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{A4}
val high = 6 :: Integer{A6}
val unused = 9 :: Integer{F9}

val c = Clock()

Rtimer(high) >> stop ; c()

{-
TYPE:  Integer
OUTPUT:
6
-}
