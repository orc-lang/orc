{- Wide.orc -- Orc program Wide
 - 
 - Created by amp on Jul 15, 2013 4:57:57 PM
 -}

include "benchmark.inc"

val n = problemSizeScaledInt(5000)

benchmarkSized("Wide", n, { Counter(n) }, {
val c = _
upto(n) >> c.dec() >> stop |
c.onZero() >> "Done!" },
{ _ = "Done!" }
)

{-
OUTPUT:
"Done!"
-}

{-
BENCHMARK
-}

