{-
  An Orc site is strict, and returns only one value.
-}

def noise(Integer) :: Integer
def noise(i) = i | Rwait(500) >> 0
val Noise = MakeSite(noise)

val x = Rwait(1000) >> 5
Rwait(250) >> 10 | Noise(x)

{-
OUTPUT:
10
5
-}
