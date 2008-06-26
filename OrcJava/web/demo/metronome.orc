-- Metronome: publish a signal every time unit.

def Metronome() = () | Rtimer(1000) >> Metronome()

Metronome()