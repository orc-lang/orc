-- Parallel-or
-- outputs the result, and how and when the sites have responded

def randBool(t) =   random(2) >x> random(t) >s> Rtimer(s) >> (x=0,s)
def signals(n) =    if(n>0) >> (signal | signals(n-1))
-- signals(10)  >> (
z <z<
(
   if(x) >> (true,"site 1:",s1) | if(y) >> (true,"site 2:",s2)
  | ((x || y),("site 1:",s1),("site 2:",s2))

    <(x,s1)< randBool(100)
    <(y,s2)< randBool(100)
)
--)
