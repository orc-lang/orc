include "timeIt.inc"

val n = 20

def makeChannels[A](n :: Integer) = collect({ upto(n) >> Channel[A]() })
def rotateList(x : xs) = append(xs, [x])

val chans = makeChannels[Integer](n)

-- TODO: Give real types.
class Connector {
  val x :: Top
  val y :: Top
  val n :: Integer
  
  val counter = Counter(n)
  
  def wait() = counter.onZero()
  
  --val _ = Println("Connecting " + x + " to " + y) >> "From inside"
  val _ = repeat(x.get) >v> y.put(v+1) >> counter.dec()
  val _ = counter.onZero() >> y.close() -- >> Println("Connector Done")
}
def Connector(x_ :: Top, y_ :: Top, n_ :: Integer) = new Connector { val x = x_ # val y = y_ # val n = n_ }

timeIt({
  each(zip(chans, rotateList(chans))) >(x, y)> Connector(x, y, 5000) >> stop -->c> c.wait()
  |
  head(chans).put(1)
  |
  head(chans).put(-100000)
})

{-
BENCHMARK
-}
