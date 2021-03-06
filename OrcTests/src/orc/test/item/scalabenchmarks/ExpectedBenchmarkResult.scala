//
// ExpectedBenchmarkResult.scala -- Scala trait ExpectedBenchmarkResult and utilities
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks

import java.lang.reflect.{Array => JArray}
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLongArray
import scala.util.hashing.MurmurHash3
import scala.collection.parallel.ParIterable

trait HashBenchmarkResult[R] {
  this: BenchmarkApplication[_, R] =>

  val expected: ExpectedBenchmarkResult[R]

  def check(results: R): Boolean = expected.check(results)
}

object ExpectedBenchmarkResult {
  def unknownArray2IndexedSeq(a: Any): IndexedSeq[Any] = {
    (0 until JArray.getLength(a)).map(JArray.get(a, _))
  }
  def unknownArray2IndexedSeq(a: Any, takeOnly: Int): IndexedSeq[Any] = {
    (0 until (JArray.getLength(a) min takeOnly+1)).map(JArray.get(a, _))
  }
}

trait ExpectedBenchmarkResult[R] {
  import ExpectedBenchmarkResult._
  
  /** Mapping from problem size to the expected hash
    *
    */
  val expectedMap: Map[Int, Int]
  
  def hashCollection(i: TraversableOnce[Int]): Int = i.## // MurmurHash3.orderedHash(i)
  def hashCollection(i: ParIterable[Int]): Int = hashCollection(i.seq)

  def expected: Int = expectedMap.get(BenchmarkConfig.problemSize) match {
    case Some(h) => h
    case None => 
      throw new AssertionError(s"Expected hash for problem size ${BenchmarkConfig.problemSize} is missing in $this.")
  }

  def hash(results: R): Int = hashInternal(results)

  def hashInternal(results: Any): Int = {
    val r = {
      results match {
        case null => 0
        case s: java.lang.Iterable[_] =>
          import collection.JavaConverters._
          hashCollection(s.asScala.par.map(hashInternal))
        case s: Iterable[_] =>
          hashCollection(s.par.map(hashInternal))
        case a if a.getClass.isArray =>
          val s = unknownArray2IndexedSeq(a)
          hashCollection(s.toStream.par.map(hashInternal))
        case a: AtomicIntegerArray =>
          val s = (0 until a.length).iterator.map(a.get(_))
          hashCollection(s.toStream.par.map(hashInternal))
        case a: AtomicLongArray =>
          val s = (0 until a.length).iterator.map(a.get(_))
          hashCollection(s.toStream.par.map(hashInternal))
        case o =>
          o.##
      }
    }
    //println(s"Hashing: $b")
    r
  }
  
  private val summaryPrefix = System.getProperty("orc.test.benchmark.summarizeResultsPrefix", "10").toInt
  
  private def summarizeResults(results: R): String = {
    val r = results match {
      case s: Iterable[_] =>
        s.take(summaryPrefix).mkString("[", ",\n", s"${if (summaryPrefix < s.size) ", ..." else ""}]] (${s.size} items)")
      case s: java.util.Collection[_] =>
        import collection.JavaConverters._
        s.asScala.take(summaryPrefix).mkString("[", ",\n", s"${if (summaryPrefix < s.size) ", ..." else ""}]] (${s.size} items)")
      case s: java.lang.Iterable[_] =>
        import collection.JavaConverters._
        s.asScala.take(summaryPrefix).mkString("[", ",\n", ", ...]")
      case a if a.getClass.isArray =>
        val s = unknownArray2IndexedSeq(a, takeOnly = summaryPrefix)
        s.mkString("[", ",\n", s"${if (s.size < JArray.getLength(a)) ", ..." else ""}] (${JArray.getLength(a)} items)")
      case o =>
        val str = o.toString
        str.take(summaryPrefix * 10) + (if (summaryPrefix * 10 < str.size) "[...]" else "")
    }
    r
  }
  
  private var previousResults: Option[Any] = None

  private val doSummarizeDifference = System.getProperty("orc.test.benchmark.summarizeDifference", "false").toBoolean

  private def summarizeDifference(results: R): String = {
    if (doSummarizeDifference) {
      def handleIterable(a: Iterable[_], b: Iterable[_]) = {
        val s = (a zip b).filter({ case (x, y) => x != y })
        s.take(summaryPrefix).map(p => s"${p._1} != ${p._2}").mkString("[", ",\n", s", ...] (${s.size} items)")
      }
      
      val r = (results, previousResults) match {
        case (a: Iterable[_], Some(b: Iterable[_])) =>
          handleIterable(a, b)
        case (a, Some(b)) if a.getClass.isArray && b.getClass.isArray =>
          val ai = unknownArray2IndexedSeq(a) // (0 until JArray.getLength(a)).map(JArray.get(a, _))
          val bi = unknownArray2IndexedSeq(b) // (0 until JArray.getLength(b)).map(JArray.get(b, _))
          handleIterable(ai, bi)
        case (_, None) =>
          "Differences can only be shown if their was a previous successful rep."
        case (_, _) =>
          "Only sequences are supported. Best of luck."
      }
      r
    } else {
      summarizeResults(results)
    }
  }

  def check(results: R): Boolean = {
    if (System.getProperty("orc.test.benchmark.summarizeResults", "false").toBoolean)
      println(s"Hash: ${hash(results).formatted("0x%08x")}\n${summarizeResults(results)}")
    if (BenchmarkConfig.checkResults) {
      lazy val actual = hash(results)
      val expected = try {
        this.expected
      } catch {
        case e: AssertionError => 
          throw new AssertionError(s"Failed to check result. The hash for problem size ${BenchmarkConfig.problemSize} -> ${actual.formatted("0x%08x")}.", e) 
      }
      val r = actual == expected
      if (!r) {
        throw new AssertionError(s"The actual result (${actual.formatted("0x%08x")}) does not match the expected (${expected.formatted("0x%08x")}) for problem size ${BenchmarkConfig.problemSize}.\n${summarizeDifference(results)}")
      }
      if (doSummarizeDifference) {
        previousResults = Some(results)
      }
      r
    } else true
  }
} 

trait UnorderedHash[A] extends ExpectedBenchmarkResult[A] {
  override def hashCollection(i: TraversableOnce[Int]): Int = MurmurHash3.unorderedHash(i)
}
