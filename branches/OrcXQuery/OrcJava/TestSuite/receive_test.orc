def receiveLoop(count) =
  Receive() >x> (let(x) | let(count) | receiveLoop(count+1))

def metronome(count) =
  Rtimer(3000) >> (let(count) | metronome(count+1))

metronome(0) | receiveLoop(0)
