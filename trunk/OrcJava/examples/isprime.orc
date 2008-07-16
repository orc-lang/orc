def isPrime(n) = 
  def primeat(i) = 
  	val b = i * i <= n
      if(b) >> (n % i /= 0) && primeat(i+1)
    | if(~b) >> true
  primeat(2)

def Metronome(i) = i | Rtimer(500) >> Metronome(i+1)
      
-- Publish only prime numbers
Metronome(2) >n> if(isPrime(n)) >> n