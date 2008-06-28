-- Metronome: publish a signal every time unit.

def Metronome(i) =
    i
  | Rtimer(1000) >> Metronome(i+1)

Metronome(0)
