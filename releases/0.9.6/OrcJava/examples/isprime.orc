def isPrime(n) = 
  def primeat(i) = 
    val b = i * i <= n
      if(b) >> (n % i /= 0) && primeat(i+1)
    | if(~b) >> true
  primeat(2)
      
-- Publish only prime numbers
each(range(1, 100)) >n> if(isPrime(n)) >> n
{-
OUTPUT:
1
2
3
5
7
11
13
17
19
23
29
31
37
41
43
47
53
59
61
67
71
73
79
83
89
97
-}