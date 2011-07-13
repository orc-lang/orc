def burst(width, rate) =
  upto(Random(width)) >> signal
| Rwait(Random(rate)) >> burst(width, rate)


def metronome(rate) =
  signal
| Rwait(rate) >> metronome(rate)

def swap(x,y) = atomic ( (x?, y?) >(a,b)> (x := b, y := a) >> signal )

val box =
  val contents = [2,3,5,7,11,13,17,19,23,29]
  def fill(i) = TRef(index(contents, i))
  Table(10, fill)

{- Shuffle refs repeatedly -}
burst(20, 10) >> swap(box(Random(10)), box(Random(10))) >> stop

|

{- Read list every 200ms -}
metronome(200) >> atomic (map (lambda (i) = box(i)?, range(0, 10)) )

