-- Multilevel add.orc -- Demonstrate a site call combining disparate argument labels
--
-- $Id$
--
-- Add values of different labels,
-- check that output has correct inferred label.
-- An example of arrow type parameter/result label inference.

val public = 1 :: Integer
val secret1 = 4 :: Integer{B4}
val secret2 = 6 :: Integer{A6}
val unused = 9 :: Integer{F9}

public + secret1 + secret2

{-
TYPE:  Integer{B6}
OUTPUT:
11
-}
