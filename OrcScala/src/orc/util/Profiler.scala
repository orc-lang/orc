//
// Profiler.scala -- Scala object Profiler
// Project OrcScala
//
// Created by jthywiss on Mar 26, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.OutputStreamWriter

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
  }
  else null

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
    final val accumulatorMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]] {
      override def default(key: Symbol): Array[Long] = null
    }

    @inline
    def add(locationId: Long, intervalType: Symbol, intervalCount: Long, intervalDurationNanos: Long) {
      val accums = accumulatorMap(intervalType)
      if (accums != null) {
        accums(0) += intervalCount
        accums(1) += intervalDurationNanos
        /* duration ^ 2 can overflow a Long, so switch to milliseconds in that case */
        val oldDurn2 = accums(2)
        if (oldDurn2 >= 0 && (oldDurn2 + intervalDurationNanos * intervalDurationNanos) >= 0) {
          accums(2) += intervalDurationNanos * intervalDurationNanos
        } else {
          if (oldDurn2 >= 0) {
            /* Just overflowed, switch to millisecond mode */
            accums(2) = -(accums(2) / 1000000000000L)
          }
          /* Now in millisecond mode */
          val intervalDurationMillis = intervalDurationNanos / 1000000L
          accums(2) -= intervalDurationMillis * intervalDurationMillis
        }
        accums(3) = Math.min(accums(3), intervalDurationNanos)
        accums(4) = Math.max(accums(4), intervalDurationNanos)
      } else {
        val newAccums = new Array[Long](5)
        newAccums(0) = intervalCount
        newAccums(1) = intervalDurationNanos
        newAccums(2) = intervalDurationNanos * intervalDurationNanos
        newAccums(3) = intervalDurationNanos
        newAccums(4) = intervalDurationNanos
        accumulatorMap.put(intervalType, newAccums)
      }
    }

    ProfilingAccumulatorsDumpThread.register(this)
  }

  private object ProfilingAccumulatorsDumpThread extends ShutdownHook("ProfilingAccumulatorsDumpThread") {
    private val accums = scala.collection.mutable.Set[ProfilingAccumulators]()
    override def run = synchronized {
      val sumMap = new scala.collection.mutable.HashMap[Symbol, Array[Long]]
      var intervalTypeColWidth = 14

      for (pa <- accums) {
        for (e <- pa.accumulatorMap) {
          val sums = sumMap.getOrElseUpdate(e._1, Array(0L, 0L, 0L, Long.MaxValue, -1L))
          sums(0) += e._2(0)
          sums(1) += e._2(1)
          sums(2) += e._2(2)
          sums(3) = Math.min(sums(3), e._2(3))
          sums(4) = Math.max(sums(4), e._2(4))
          if (e._1.name.length > intervalTypeColWidth) intervalTypeColWidth = e._1.name.length
        }
      }

      /* Convention: synchronize on System.err during output of block */
      System.err synchronized {
        System.err.append(s"Profiling Accumulators: begin, ${sumMap.size} entries\n")
        System.err.append("Interval-Type".padTo(intervalTypeColWidth, '-'))
        System.err.append("\t-------Count--------\t--Accum.-Time-(ns)--\tAccum.-Time^2-(ns^2)\t---Min.-Time-(ns)---\t---Max.-Time-(ns)---\n")

        for (e <- sumMap) {
          System.err.append(e._1.name.padTo(intervalTypeColWidth, ' '))
          System.err.append(f"\t${e._2(0)}%20d\t${e._2(1)}%20d\t${if (e._2(2) >= 0) e._2(2).toDouble else (e._2(2).toDouble * -1e12)}%20.15g\t${e._2(3)}%20d\t${e._2(4)}%20d\n")
        }
        System.err.append(f"Profiling Accumulators: end\n")
      }

      val csvOut = ExecutionLogOutputStream("profile", "csv", "Profile output file")
      if (csvOut.isDefined) {
        val profileCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
        val csvWriter = new CsvWriter(profileCsv.append(_))
        val tableColumnTitles = Seq("Interval Type", "Count", "Accum. Time (ns)", "Accum. Time^2 (ns^2)", "Min. Time (ns)", "Max. Time (ns)")
        csvWriter.writeHeader(tableColumnTitles)
        val rows = sumMap.map(e => ((e._1.name, e._2(0), e._2(1), if (e._2(2) >= 0) e._2(2).toDouble else (e._2(2).toDouble * -1e12), e._2(3), e._2(4))))
        csvWriter.writeRows(rows)
        profileCsv.close()
      }
    }

    def register(pa: ProfilingAccumulators) = synchronized {
      accums += pa
    }
  }

  if (profilerOn) Runtime.getRuntime().addShutdownHook(ProfilingAccumulatorsDumpThread)

}
