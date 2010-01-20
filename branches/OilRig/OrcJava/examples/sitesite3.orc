{-
  An Orc site is strict, and returns only one value.
-}

def noise(Integer) :: Integer
def noise(i) = i | Rtimer(500) >> 0 
val Noise = Site(noise)

val x = Rtimer(1000) >> 5
Rtimer(250) >> 10 | Noise(x)
 
{-
OUTPUT:
10
5
-}