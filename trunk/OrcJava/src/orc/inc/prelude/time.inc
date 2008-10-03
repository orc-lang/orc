site Rtimer = orc.lib.time.Rtimer
site MakeTimer = orc.lib.time.MakeTimer
site Clock = orc.lib.time.Clock

{- Define a default logical timer -}
val Ltimer = MakeTimer()

def Metronome() = signal | Rtimer(1000) >> Metronome()
def MetronomeT(t) = signal | Rtimer(t) >> MetronomeT(t)
