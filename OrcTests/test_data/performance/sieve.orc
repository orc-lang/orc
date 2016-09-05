{- sieve.orc -- Orc program implementing Eratosthenes's sieve

Eratosthenes's Sieve is an algorithm for finding prime numbers.
In an imperative setting, it works as follows:

# start with a list of natural numbers from 2 to n (some number)
# remove and output the first number of the list (call it p)
# remove all multiples of p from the list
# repeat steps 2 and 3 until the list is empty

Note that step 3 can begin removing multiples starting with p
squared (all lower multiples are already removed), and once p &gt;
square root of n, the remaining numbers in the list are prime.
We can parallelize this algorithm in two easy ways:

# step 3 can be done in parallel for all multiples of p
# given a list of prime numbers, step 2 can be done in parallel
for those prime numbers

Implement a parallel version of this algorithm using these facts.

Note: you can compute the "floored" square root of a number like this:

Floor(sqrt(n))
-}

include "timeIt.inc"

import class Set = "java.util.Set"
import class HashSet = "java.util.HashSet"
import class Collections = "java.util.Collections"

def primes(Number) :: List[Number]
def primes(n) =
  def candidates(n :: Number) = rangeBy(3, n+1, 2)
  def sieve(Number, Set[Number]) :: List[Number]
  def sieve(1, _) = []
  def sieve(n, set) =
    def remove(p :: Number) = map(set.remove, rangeBy(p*p, n, p))
    sieve(Floor(sqrt(n)), set) >ps>
    map(remove, ps) >>
    2:filter(set.contains, candidates(n))
  val set = Collections.synchronizedSet[Number](HashSet[Number]())
  map(set.add, candidates(n)) >>
  sieve(n, set)

timeIt(lambda() =
primes(25000)
)

{-
BENCHMARK
-}
