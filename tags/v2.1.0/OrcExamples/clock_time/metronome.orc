{- metronome.orc -- Orc program that publishes periodically
 -}

def metronomeTN(t :: Integer, n :: Integer) :: Integer = n | Rwait(t) >> metronomeTN(t, n+1)

metronomeTN(1000, 1)
