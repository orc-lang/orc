site Rtimer = orc.lib.time.Rtimer
site MakeTimer = orc.lib.time.MakeTimer

{- Define a default logical timer -}
val Ltimer = MakeTimer()

def Metronome() = () | Rtimer(1000) >> Metronome()
def MetronomeT(t) = () | Rtimer(t) >> MetronomeT(t)
