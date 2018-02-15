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

package orc.test.item.scalabenchmarks

import java.util.{ Collections, HashSet, Set }

import scala.collection.JavaConverters.asScalaSetConverter

object Sieve extends BenchmarkApplication[Unit, Iterable[Long]] with ExpectedBenchmarkResult[Iterable[Long]] {
  val N = BenchmarkConfig.problemSizeScaledInt(20000)

  def primes(n: Long): List[Long] = {
    def candidates(n: Long) = 3L until (n + 1) by 2
    def sieve(n: Long, set: Set[Long]): List[Long] = n match {
      case n if n == 1 => List()
      case n =>
        def remove(p: Long) = ((p * p) until (n + 1) by p).foreach(set.remove)
        val ps = sieve(math.floor(math.pow(n.toDouble, 0.5)).toLong, set)
        ps.foreach(remove)
        2L :: set.asScala.toList
    }
    val set = Collections.synchronizedSet[Long](new HashSet[Long]())
    candidates(n).foreach(set.add)
    sieve(n, set)
  }

  def benchmark(ctx: Unit) = {
    primes(N)
  }

  val name: String = "Sieve"

  val size: Int = N

  def setup(): Unit = ()

  override def hash(results: Iterable[Long]): Int = results.toSet.##()

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0xae7d25a5,
      10 -> 0x2a82d246,
      100 -> 0xbb1da227,
      )
}
