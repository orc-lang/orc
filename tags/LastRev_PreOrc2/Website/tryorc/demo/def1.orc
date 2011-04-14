def gen(i,t) = i | Rtimer(t) >> gen(i+1,t)

-- gen(2,1000)

-- gen(2,1000) | gen(100,2000)
