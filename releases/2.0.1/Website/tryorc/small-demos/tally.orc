{-
Call some simulated sites and count the number which respond within a fixed
time limit.
-}
val n = 10 -- number of sites
val t = 12  -- max time within which a site responds
val p = 0.9 -- probability of response

def biasedBool(p) = Random(1000) >x> (x <= p*1000)
def rrandSignal(t,p) = Ift(biasedBool(p)) >> Random(t) >s> Rwait(s)

def tally(0) =   0
def tally(k) =  (rrandSignal(t,p) >> 1 | Rwait(10) >> 0) + tally(k-1)

tally(n)
