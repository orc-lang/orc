{- Wide.orc -- Orc program Wide
 - 
 - $Id$
 - 
 - Created by amp on Jul 15, 2013 4:57:57 PM
 -}

include "timeIt.inc"

val n = 10000
val c = Counter(n)

upto(n) >> c.dec() >> stop |
c.onZero() >> "Done!"

{-
BENCHMARK
-}

