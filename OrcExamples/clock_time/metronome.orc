{- metronome.orc -- Orc program that publishes periodically
 -}

def metronomeTN(t, n) = n | Rwait(t) >> metronomeTN(t, n+1)

metronomeTN(1000, 1)
