val n = 10 -- number of sites
val t= 12  -- max time within which a site responds
val p = 0.9 -- probability of response
def tally(0) =   0
def tally(k) =  (rrandSignal(t,p)>>  1 | Rtimer(10) >> 0) + tally(k-1)

tally(n)
