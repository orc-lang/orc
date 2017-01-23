/* sieve.orc -- Orc program implementing Eratosthenes's sieve

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
*/

package orc.scalabenchmarks

import java.util.{ HashSet, Set }
import scala.collection.JavaConversions._
import java.util.Collections
import orc.test.BenchmarkApplication

object Sieve extends BenchmarkApplication {

  def primes(n: BigInt): List[BigInt] = {
    def candidates(n: BigInt) = BigInt(3) until (n + 1) by 2
    def sieve(n: BigInt, set: Set[BigInt]): List[BigInt] = n match {
      case n if n == 1 => List()
      case n =>
        def remove(p: BigInt) = ((p * p) until (n + 1) by p).foreach(set.remove)
        val ps = sieve(BigInt(math.floor(math.pow(n.toDouble, 0.5)).toLong), set)
        ps.foreach(remove)
        BigInt(2) :: set.toList
    }
    val set = Collections.synchronizedSet[BigInt](new HashSet[BigInt]())
    candidates(n).foreach(set.add)
    sieve(n, set)
  }

  def main(args: Array[String]): Unit = {
    println(Util.timeIt { primes(25000) })
  }

}
