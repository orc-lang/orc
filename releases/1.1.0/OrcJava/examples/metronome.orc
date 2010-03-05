def metronomeTN(t, n) = n | Rtimer(t) >> metronomeTN(t, n+1)

metronomeTN(1000, 1)