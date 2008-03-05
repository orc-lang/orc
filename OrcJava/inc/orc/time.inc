site Rtimer = orc.lib.time.Rtimer

def Metronome() = () | Rtimer(1000) >> Metronome()
