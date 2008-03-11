site Rtimer = orc.lib.time.Rtimer
site Ltimer = orc.lib.time.Ltimer

def Metronome() = () | Rtimer(1000) >> Metronome()
