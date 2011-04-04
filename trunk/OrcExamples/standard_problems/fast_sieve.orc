{- fast_sieve.orc
 - 
 - $Id$
 - 
 - Created by amshali on Mar 31, 2010 10:57:33 PM
 -}

-- a channel to store the primes generated
val prime_buf = Channel()

-- a sieve class with value n represent a prime number n
-- which immediately print itself.
-- this algorithm for generating primes is inspired
-- by the fact that if square(p) > input v then v is a prime.
def class fast_sieve(n) =
  val p = n
  val psq = n * n -- square of n
  val next = Cell() -- next sieve, initially empty
  val in_buff = Channel() -- input channel
  def in(v) = in_buff.put(v)
  def main() = 
    in_buff.get() >v> ( 
    -- if v is divisible by this sieve then discard it 
    if v % p = 0 then signal
    -- if the psq is greater than v then v should be prime
    -- so put the sieve(v) in prime channel. If the next sieve
    -- is empty then get a prime from prime channel and assign
    -- it to next
    -- if the psq is less than v then just send the v to the next sieve 
    else (
      if psq :> v then prime_buf.put(fast_sieve(v)) >> next.readD() ; next.write(prime_buf.get())
      else next.read() >s> s.in(v)
    )) >> main()

  Println(n) | main()

-- the first prime number is 2 
val s2 = fast_sieve(2)

val MAX = 100

def gen_primes(n) =
  if (n <: MAX) then 
    s2.in(n) >> gen_primes(n+2)
  else stop

gen_primes(3)

{-
OUTPUT:
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
