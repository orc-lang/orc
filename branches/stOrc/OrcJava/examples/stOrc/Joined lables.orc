-- Joined labels.orc -- Demonstrate the comination of various labels
--
-- $Id$
--
-- Expression produces values at multiple levels.
-- Check that the expression's type is the highest
-- label of any value produced.

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

low | mid | high

{-
TYPE:  Integer{6}
OUTPUT:
1
4
6
-}
