def isPrime(n) =
  def primeat(i) =
      if(b) >> (n % i /= 0) && primeat(i+2)
    | if(~b) >> true
        <b< i * i <= n
  primeat(3)
{- EXAMPLE -}
def Metronome(i) =
    i
  | Rtimer(1000) >> Metronome(i+2)

-- Publish only prime numbers
2 | Metronome(3) >n> if(isPrime(n)) >> n
