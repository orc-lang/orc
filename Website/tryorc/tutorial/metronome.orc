-- metronomeN counts the number of seconds that have passed,
-- publishing a number each second
def metronomeN(i) =
    i
  | Rwait(1000) >> metronomeN(i+1)

metronomeN(0)
