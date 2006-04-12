-- run two metronomes

def Metronome(x) =
	let(x) | Rtimer(x) >> Metronome(x)

Metronome(2000) | Metronome(3000)
