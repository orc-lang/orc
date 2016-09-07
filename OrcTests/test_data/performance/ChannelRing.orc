include "timeIt.inc"

val n = 20

def makeChannels[A](n :: Integer) = collect(lambda() = upto(n) >> Channel[A]())
def rotateList(x : xs) = append(xs, [x])

val chans = makeChannels[Integer](n)

def Connector(x, y, n) =
  val counter = Counter(n) #
  def wait() = counter.onZero() #
  
  -- Println("Connecting " + x + " to " + y) >> "From inside" |
  (
    repeat(x.get) >v> y.put(v+1) >> counter.dec() |
    counter.onZero() >> y.close() >> Println("Connector Done")
  ) >> stop

timeIt(lambda() =
  each(zip(chans, rotateList(chans))) >(x, y)> Connector(x, y, 2000) -->c> c.wait()
  |
  head(chans).put(1)
  |
  head(chans).put(-100000)
)

{-
BENCHMARK
-}