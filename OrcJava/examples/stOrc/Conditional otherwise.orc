-- Conditional otherwise.orc -- Demonstrate another control flow dependency in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{A4}
val high = 6 :: Integer{A6}
val unused = 9 :: Integer{F9}

(if(high > 2) >> "high > 2") ; "high <= 2"

{-
TYPE:  String{A6}
OUTPUT:
"high > 2"
-}
