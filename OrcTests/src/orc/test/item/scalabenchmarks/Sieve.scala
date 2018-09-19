//
// Sieve.scala -- Scala benchmark Sieve implementing Eratosthenes's sieve
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

/*
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

import java.util.{ Collections, HashSet, Set }

import scala.collection.JavaConverters.asScalaSetConverter
import scala.concurrent.Channel
import java.util.ArrayList

object Sieve extends BenchmarkApplication[Unit, Iterable[Long]] with ExpectedBenchmarkResult[Iterable[Long]] {
  val N = BenchmarkConfig.problemSizeScaledInt(5000)

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
      1 -> 0xad0c3633,
      10 -> 0x348f29a9,
      100 -> 0x10dba805,
      )
}


object SavinaSieve extends BenchmarkApplication[Unit, Iterable[Long]] with HashBenchmarkResult[Iterable[Long]] {
  import Util._
  import collection.JavaConverters._

  // Lines: 1
  def check(list: ArrayList[Long], x: Long) = !list.asScala.exists(x % _ == 0)

  val sieveFragementSize = 300

  // Lines: 27 (1)
  def sieveFragment(outChan: Channel[Long]): Channel[Long] = {
    val inChan = new Channel[Long]()
    val list = new ArrayList[Long](sieveFragementSize)
    var next: Channel[Long] = null
    def filter(x: Long) = {
      val v = check(list, x)
      if (v) {
        if (list.size() < sieveFragementSize) {
          list.add(x)
          outChan.write(x)
        } else {
          // create a new fragment
          if (next == null) {
            next = sieveFragment(outChan)
          }
          next.write(x)
        }
      }
    }
    //println("Creating new fragment: " + inChan)
    thread {
      var isDone = false
      while (!isDone) {
        val x = inChan.read
        if (x >= 0)
          filter(x)
        else {
          isDone = true
          if (next != null)
            next.write(x)
          else
            outChan.write(x)
        }
      }
    }
    inChan
  }

  // Lines: 7
  def primes(n: Long): List[Long] = {
    def candidates(n: Long) = 3L until (n + 1) by 2
    val out = new Channel[Long]()
    val filter = sieveFragment(out)
    candidates(n).foreach(filter.write)
    filter.write(-1)
    2 :: Stream.continually(out.read).takeWhile(_ > 0).toList
  }

  def benchmark(ctx: Unit) = {
    primes(Sieve.N)
  }

  val name: String = "Sieve-savina"

  val size: Int = Sieve.N

  def setup(): Unit = ()

  val expected = Sieve
}
