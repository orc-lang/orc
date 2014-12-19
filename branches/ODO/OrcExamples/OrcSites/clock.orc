{- clock.orc -- Orc program: Time a delay
 -}

include "date.inc"

def clock() =
  val start = DateTime().getMillis()
  { DateTime().getMillis() - start }

clock() >c> Rwait(1000) >> c()
