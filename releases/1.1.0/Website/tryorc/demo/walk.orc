{- Random Walk: Starting at a wall, randomly move one step
toward or away from the wall every time unit.  Count the
number of time units required to get 10 steps away from the
wall. -}

{--
dir() is a coin toss that yields -1 or 1 with equal probability.
--}
def dir() = random(2) >x> (if(x=0) >> -1 | if (x=1) >> 1)

{--
randomWalk(current,final) yields the number of steps in a
run to reach the final position, final, starting from the
current position, current. Position 0 has a hard wall to its
left; so, the next move from 0 is always to position 1.
Assume that final > 0.
--}
def randomWalk(current,final) =
   if(current = final) >> 0
 | if(0 < current && current < final) >> Rtimer(10) >>
      1 + randomWalk(current + dir(), final)
 | if(current = 0) >> Rtimer(10) >> 1 + randomWalk(1,final)

-- Test
randomWalk(0,10)
