def rrtimer(bound) = random(bound) >x> Rtimer(x) >> x
def rbool() = random(2) >x> (x = 0)
{- EXAMPLE -}
{-
Parallel-or:
Flip two coins (true or false) with a random delay;
Publish the OR, which sites responded, and how long they took.
Repeat the experimen 10 times.
-}

signals(10)  >> (
  z <z< (
      if(x) >> (true,("site 1:",t1))
    | if(y) >> (true,("site 2:",t2))
    | ((x || y),("site 1:",t1),("site 2:",t2))
  
      <(x,t1)< (rbool(), rrtimer(10))
      <(y,t2)< (rbool(), rrtimer(10))
  )
)
