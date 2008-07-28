def isPrime(n) = 
  def primeat(i) = 
      if(b) >> (n % i /= 0) && primeat(i+1)
    | if(~b) >> true
        <b< i * i <= n
  primeat(2)

def Metronome(i) =
    i
  | Rtimer(1000) >> Metronome(i+1)

-- Publish only prime numbers
Metronome(2) >n> if(isPrime(n)) >> n
