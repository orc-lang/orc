{- 
Experiments with Dice:

Roll a pair of dice n times and count the number of times the
total shown by the two dice is c. experiment(n,c) returns this value.
-}

def throw() = Random(6) + 1 -- roll of a single die

def experiment(0,_) = 0
def experiment(n,c) =
   val score = if throw() + throw() = c then 1 else 0
   -- Goal expression for experiment(n,c)
   score + experiment(n-1,c)

-- Test
experiment(60,7)
