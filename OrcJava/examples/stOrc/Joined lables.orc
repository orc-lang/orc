-- Joined labels.orc -- Demonstrate the combination of various labels
--
-- $Id$
--
-- Expression produces values at multiple levels.
-- Check that the expression's type is the highest
-- label of any value produced.

val public = 1 :: Integer
val secret1 = 4 :: Integer{C4}
val secret2 = 6 :: Integer{A6}
val unused = 9 :: Integer{F9}

public | secret1 | secret2

{-
TYPE:  Integer{C6}
OUTPUT:
1
4
6
-}
