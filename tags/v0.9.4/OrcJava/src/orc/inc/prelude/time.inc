site Rtimer = orc.lib.time.Rtimer
site MakeTimer = orc.lib.time.MakeTimer

{- Define a default logical timer -}
val Ltimer = MakeTimer()

def Metronome() = signal | Rtimer(1000) >> Metronome()
def MetronomeT(t) = signal | Rtimer(t) >> MetronomeT(t)
