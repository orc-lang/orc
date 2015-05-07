{--
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

class Math = java.lang.Math
Math.floor(Math.sqrt(5))
--}

def sqrt(Number) :: Integer
def sqrt(n) =
  class Math = java.lang.Math
  (Math.floor(Math.sqrt(n.doubleValue()))).intValue()

def primes(Number) :: List[Number]
def primes(n) =
  def candidates(n :: Number) = rangeBy(3, n+1, 2)
  def sieve(Number, Set[Number]) :: List[Number]
  def sieve(1, _) = []
  def sieve(n, set) =
  	def remove(Number) :: Top
    def remove(p) = joinMap(set.remove, rangeBy(p*p, n, p))
    sieve(sqrt(n), set) >ps>
    joinMap(remove, ps) >>
    2:filter(set.contains, candidates(n))
  val set = Set[Number]()
  joinMap(set.add, candidates(n)) >>
  sieve(n, set)

primes(100)

{-
OUTPUT:
[2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97]
-}