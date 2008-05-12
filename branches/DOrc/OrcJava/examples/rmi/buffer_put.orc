val b = Remote("buffer")
def Count(i) = i | if(i /= 0) >> Rtimer(1000) >> Count(i-1)
Count(10) >x> b.put(x) >> x