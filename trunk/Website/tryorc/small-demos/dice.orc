{-- Experiments with Dice:
Roll a pair of dice n times and count the number of times the
total shown by the two dice is c. exp(n,c) returns this value. --}

def exp(0,_) = 0

def exp(n,c) =
   def throw() = random(6) + 1 -- roll of a single dice

   -- Goal expression for exp(n,c)
   (if throw() + throw() = c then 1 else 0) + exp(n-1,c)

-- Test
exp(60,7)
