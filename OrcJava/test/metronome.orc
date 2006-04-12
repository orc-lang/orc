-- sends a signal ever once in a while...

def Metronome(x) = 
  let(x) | Rtimer(3000) >> add(x, 1) >y> Metronome(y)

Metronome(0)
