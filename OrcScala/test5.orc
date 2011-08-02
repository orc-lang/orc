def inc(x) = x := x? + 1

metronome(1000) >>
TRef(0) >t> 
inc(t) ++ inc(t) ++ inc(t) >> 
t?