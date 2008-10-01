{-
Random walk:
Starting at a wall, move 1 step towards or away from the wall
each time unit. Count the number of time units required to get
10 steps away from the wall. Repeat the experiment 10 times.
-}
-- coin() is coin toss that yields -1 or 1
def coin() = random(2) >x> (if (x =0) >> (-1) | if(x=1) >>1)

{- rw(cp,ns,rt) yields the number of steps to reach rt from the
current position cp given that ns steps have already been taken.
Position 0 has hard wall to its left; so a toss of -1 is interpreted
as stay at position.
-}

def rw(cp,ns,rt) = if(cp=rt) >> ns
                 |if(0<cp && cp<rt) >> Rtimer(10) >> rw(cp+coin(),ns+1,rt)
                 |if(cp=0) >> Rtimer(10) >> rw(1,ns+1,rt)
                
signals(10) >> rw(0,0,10)

