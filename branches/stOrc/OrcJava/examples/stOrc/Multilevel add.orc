-- Multilevel add.orc -- Demonstrate a site call combining disparate argument labels
--
-- $Id$
--
-- Add values of different labels,
-- check that output has correct inferred label.
-- An example of arrow type parameter/result label inference.

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

low+mid+high

{-
TYPE:  Integer{6}
OUTPUT:
11
-}
