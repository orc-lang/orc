-- Declassification.orc -- Demonstrate declassification of a level B4 value to a level A0 (public) value
--
-- $Id$

val low = 1 :: Integer
val mid = 4 :: Integer{B4}
val high = 6 :: Integer{A6}
val unused = 8 :: Integer{A8}

def declassify(secret::Integer{E6})::Integer{} = secret:!:Integer{}

declassify(mid) >x> x+100

{-
TYPE:  Integer{A0}
OUTPUT:
104
-}
