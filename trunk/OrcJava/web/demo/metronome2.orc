-- Running two metronomes in parallel

def Metronome(i,t) = i | Rtimer(t) >> Metronome(i+1,t)

Metronome(0, 2000) | Metronome(100, 3000)