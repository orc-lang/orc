{- true-random.orc -- Orc program that fetches "true" random numbers from random.org
 -}

include "net.inc"

signals(5) >> TrueRandom(1,10)
