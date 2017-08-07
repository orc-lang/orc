//
// Profiler.scala -- Scala object Profiler
// Project OrcScala
//
// Created by jthywiss on Mar 26, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

/** Rudimentary profiling facility.
  *
  * Each thread accumulates times in a thread-local profiling map.  At JVM
  * shutdown, the profiling maps are dumped to stdout.
  *
  * Times are accumulated per "interval type"s, which are [[scala.Symbol]]s.
  * Measure an interval by enclosing code in a `beginInterval`/`endInterval`
  * pair, or by using `measureInterval`.
  *
  * (Location ID Longs are taken as arguments, but not currently used.)
  *
  * Differentiating usage of Logger, Tracer, and Profiler: Logging is intended
  * for abnormal or significant events.  Tracing is intended for recording
  * routine events on an object for debugging.  Profiling is intended for
  * performance measurement.
  *
  * @author jthywiss
  */
object Profiler {
  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val profilerOn = false

  private final val profilingAccumulatorsTL = if (profilerOn) new ThreadLocal[ProfilingAccumulators]() {
    override protected def initialValue = new ProfilingAccumulators(Thread.currentThread().getId)
  } else null

  type IntervalBeginning = Long

  @inline
  def beginInterval[A](locationId: Long, intervalType: Symbol): IntervalBeginning = {
    if (profilerOn) System.nanoTime else 0L
  }
 
  @inline
  def endInterval[A](locationId: Long, intervalType: Symbol, beginning: IntervalBeginning) = {
    if (profilerOn) {
      val endTime = System.nanoTime
      profilingAccumulatorsTL.get().add(locationId, intervalType, 1L, endTime - beginning)
    }
  }

  // TODO: PERFORMANCE: Consider removing this since it will introduce an additional megamorphic call site in the java code in many cases. Make people use begine/endInterval.
  @inline /* sadly, Scala can't inline because of the try-finally block */
  def measureInterval[A](locationId: Long, intervalType: Symbol)(block: => A): A = {
    if (profilerOn) {
      val beginning = beginInterval(locationId, intervalType)
      try {
        block
      } finally {
        endInterval(locationId, intervalType, beginning)
      }
    } else {
      block
    }
  }

  @inline
  private final class ProfilingAccumulators(val javaThreadId: Long) {
    final val accumulatorMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]]

    /* Thomas Wang, "Integer Hash Function". http://web.archive.org/web/20071223173210/http://www.concentric.net/~Ttwang/tech/inthash.htm */
    /* Works well for our location IDs, even in small hash tables */
    @inline
    private def hashLongToInt(in: Long) = {
      var hash = (~in) + (in << 18)  // hash = (hash << 18) - hash - 1
      hash = hash ^ (hash >>> 31)
      hash = hash * 21               // hash = (hash + (hash << 2)) + (hash << 4);
      hash = hash ^ (hash >>> 11)
      hash = hash + (hash << 6)
      hash = hash ^ (hash >>> 22)
      hash.toInt
    }

    @inline
    def add(locationId: Long, intervalType: Symbol, intervalCount: Long, intervalDurationNanos: Long) {
      val accums = accumulatorMap.get(intervalType)
      if (accums.isDefined) {
        accums.get(0) += intervalCount
        accums.get(1) += intervalDurationNanos
      } else {
        val newAccums = new Array[Long](2)
        newAccums(0) = intervalCount
        newAccums(1) = intervalDurationNanos
        accumulatorMap.put(intervalType, newAccums)
      }
    }

    ProfilingAccumulatorsDumpThread.register(this)
  }

  private object ProfilingAccumulatorsDumpThread extends Thread("ProfilingAccumulatorsDumpThread") {
    private val accums = scala.collection.mutable.Set[ProfilingAccumulators]()
    override def run = synchronized {
      val sumMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]]
      var intervalTypeColWidth = 14

      for (pa <- accums) {
        for (e <- pa.accumulatorMap) {
          val sums = sumMap.getOrElseUpdate(e._1, Array(0L, 0L))
          sums(0) += e._2(0)
          sums(1) += e._2(1)
          if (e._1.name.length > intervalTypeColWidth) intervalTypeColWidth = e._1.name.length
        }
      }

      /* Convention: synchronize on System.err during output of block */
      System.err synchronized {
        System.err.append(s"Profiling Accumulators: begin, ${sumMap.size} entries\n")
        System.err.append("Interval-Type".padTo(intervalTypeColWidth, '-'))
        System.err.append("\t-------Count--------\t--Accum.-Time-(ns)--\n")

        for (e <- sumMap) {
          System.err.append(e._1.name.padTo(intervalTypeColWidth, ' '))
          System.err.append(f"\t${e._2(0)}%20d\t${e._2(1)}%20d\n")
        }
        System.err.append(f"Profiling Accumulators: end\n")
      }
    }

    def register(pa: ProfilingAccumulators) = synchronized {
      accums += pa
    }
  }

  if (profilerOn) Runtime.getRuntime().addShutdownHook(ProfilingAccumulatorsDumpThread)

}
