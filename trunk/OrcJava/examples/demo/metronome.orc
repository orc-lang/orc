-- Metronome: a useful expression definition

def Metronome() = () | Rtimer(1000) >> Metronome()

Metronome()