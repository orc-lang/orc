def inc(x) = atomic (x := x? + 1)

upto(20) >>
TRef(0) >t> 
inc(t) & inc(t) & inc(t) >> 
atomic (inc(t) & inc(t) & (atomic (inc(t) & inc(t)))) >> 
t?