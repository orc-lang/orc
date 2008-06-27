-- Running two metronomes in parallel

def Metronome(n,t) = n | Rtimer(t) >> Metronome(n,t)

Metronome(0, 2000) | Metronome(100, 3000)