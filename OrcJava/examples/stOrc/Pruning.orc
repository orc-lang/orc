-- Pruning.orc -- Demonstrate the pruning combinator in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{4}
val high = 6 :: Integer{6}
val unused = 9 :: Integer{9}

low <x< high

{-
TYPE:  Integer
OUTPUT:
1
-}
