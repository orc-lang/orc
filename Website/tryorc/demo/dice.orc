{-
Experiments with Dice:
Roll a pair of dice 60 times and count the number of times the
total is 7. Repeat the experiment 10 times.
-}
val m = 60 -- number of dice pair rolls in each experiment
val c = 7 -- sum of the dice rolls; expect around m/6 for answer
def dice(0) =   0
def dice(n) =
    ( (if(b) >> 1 | if(~b) >> 0)
         <b< u+v+2 = c <u< random(6) <v< random(6) )
  + dice(n-1)

signals(10) >> dice(m) -- run 10 experiments
