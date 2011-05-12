{- Random Walk: Starting at a wall, randomly move one step
toward or away from the wall every time unit.  Count the
number of time units required to get 10 steps away from the
wall. -}

{--
dir() is a coin toss that yields -1 or 1 with equal probability.
--}
def dir() = 2*Random(2) - 1

{--
randomWalk(current,final) yields the number of steps in a
run to reach the final position, final, starting from the
current position, current. Position 0 has a hard wall to its
left; so, the next move from 0 is always to position 1.
Assume that final > 0.
--}
def randomWalk(current,final) =
   Ift(current = final) >> 0
 | Ift(0 <: current && current <: final) >> Rwait(10) >>
      1 + randomWalk(current + dir(), final)
 | Ift(current = 0) >> Rwait(10) >> 1 + randomWalk(1,final)

-- Test
randomWalk(0,10)
