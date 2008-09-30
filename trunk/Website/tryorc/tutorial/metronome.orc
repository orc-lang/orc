-- Metronome counts the number of seconds that have passed,
-- publishing a number each second
def Metronome(i) =
    i
  | Rtimer(1000) >> Metronome(i+1)

Metronome(0)
