def isPrime(n) =
  def primeat(i) =
      if(b) >> (n % i /= 0) && primeat(i+2)
    | if(~b) >> true
        <b< i * i <= n
  primeat(3)
{- EXAMPLE -}
def metronomeN(i) =
    i
  | Rtimer(1000) >> metronomeN(i+2)

-- Publish only prime numbers
2 | metronomeN(3) >n> if(isPrime(n)) >> n
