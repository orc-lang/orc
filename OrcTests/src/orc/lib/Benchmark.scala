//
// Benchmark.scala -- Scala object Benchmark and Orc sites StartBenchmark and EndBenchmark
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib

import orc.Invoker
import orc.values.sites.IllegalArgumentInvoker
import java.lang.management.ManagementFactory
import orc.DirectInvoker
import orc.OrcRuntime
import collection.JavaConverters._
import orc.values.sites.Site

object Benchmark {
  val osmxbean = ManagementFactory.getOperatingSystemMXBean() match {
    case v: com.sun.management.OperatingSystemMXBean => v
    case _ => throw new AssertionError("Benchmarking requires com.sun.management.OperatingSystemMXBean")
  }

  val compilermxbean = ManagementFactory.getCompilationMXBean()
  val gcmxbean = ManagementFactory.getGarbageCollectorMXBeans()

  def getGCTime() = gcmxbean.asScala.map(_.getCollectionTime).sum

  def getTimes(): (Long, Long, Long, Long) = {
    (System.nanoTime(), osmxbean.getProcessCpuTime, compilermxbean.getTotalCompilationTime, getGCTime())
  }

  def nsToS(ns: Long) = ns.toDouble / 1000 / 1000 / 1000
  def msToS(ns: Long) = ns.toDouble / 1000

  @inline
  final val StartBenchmark = 201L
  orc.util.Tracer.registerEventTypeId(StartBenchmark, "StrtBenc")
  @inline
  final val EndBenchmark = 202L
  orc.util.Tracer.registerEventTypeId(EndBenchmark, "EndBenc ")

  @inline
  def traceStartBenchmark(): Unit = {
    orc.util.Tracer.trace(StartBenchmark, 0, 0, 0)
  }

  @inline
  def traceEndBenchmark(): Unit = {
    orc.util.Tracer.trace(EndBenchmark, 0, 0, 0)
  }


  def endBenchmark(start: (Long, Long, Long, Long), iteration: Int, size: Double) = {
    val end = Benchmark.getTimes()
    BenchmarkTimes(iteration, nsToS(end._1 - start._1), nsToS(end._2 - start._2), msToS(end._3 - start._3), msToS(end._4 - start._4), size)
  }

  val maxFraction: Double = 1.0 / 100
  val waitTime = 1000
  val maxWaits = 5

  def waitForCompilation(): Unit = {
    def h(last: Long, i: Long): Unit = {
      val now = compilermxbean.getTotalCompilationTime
      if (i < maxWaits && (now - last).abs > waitTime * maxFraction) {
        if (i > 1) {
          println(s"Waiting for compilation to finish (up to ${((maxWaits - i) * waitTime / 1000).toInt} more seconds)...")
        }
        Thread.sleep(waitTime)
        h(now, i+1)
      }
    }
    h(0, 0)
  }
}

object StartBenchmark extends Site {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      new DirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target == StartBenchmark && arguments.length == 0
        }

        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          Benchmark.traceStartBenchmark()
          System.gc()
          Benchmark.getTimes()
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

case class BenchmarkTimes(iteration: Int, runTime: Double, cpuTime: Double, compilationTime: Double, gcTime: Double, problemSize: Double)

object EndBenchmark extends Site {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 3) {
      new DirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target == EndBenchmark && arguments.length == 3
        }

        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          val start = arguments(0).asInstanceOf[(Long, Long, Long, Long)]
          val iteration = arguments(1).asInstanceOf[Number]
          val size = arguments(2).asInstanceOf[Number]
          Benchmark.traceEndBenchmark()
          Benchmark.endBenchmark(start, iteration.intValue(), size.doubleValue())
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}
