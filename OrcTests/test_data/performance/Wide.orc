{- Wide.orc -- Orc program Wide
 - 
 - Created by amp on Jul 15, 2013 4:57:57 PM
 -}

include "benchmark.inc"

-- TODO: Change back to 80000 when PorcE can handle it.
val n = 5000

benchmark({
val c = Counter(n)

upto(n) >> c.dec() >> stop |
c.onZero() >> "Done!"
})

{-
OUTPUT:
"Done!"
-}

{-
BENCHMARK
-}

