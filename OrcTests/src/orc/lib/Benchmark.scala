package orc.lib

import orc.values.sites.InvokerMethod
import orc.Invoker
import orc.IllegalArgumentInvoker
import java.lang.management.ManagementFactory
import orc.OnlyDirectInvoker

object Benchmark {
  val osmxbean = ManagementFactory.getOperatingSystemMXBean() match {
    case v: com.sun.management.OperatingSystemMXBean => v
    case _ => throw new AssertionError("Benchmarking requires com.sun.management.OperatingSystemMXBean")
  }

  def getTimes(): (Long, Long) = (System.nanoTime(), osmxbean.getProcessCpuTime)
}

object StartBenchmark extends InvokerMethod {
  def getInvoker(args: Array[AnyRef]): Invoker = {
    if (args.length == 0) {
      new OnlyDirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target == StartBenchmark && arguments.length == 0
        }

        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          Benchmark.getTimes()
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}

case class BenchmarkTimes(iteration: Int, runTime: Double, cpuTime: Double)

object EndBenchmark extends InvokerMethod {
  def nsToS(ns: Long) = ns.toDouble / 1000 / 1000 / 1000
  
  def getInvoker(args: Array[AnyRef]): Invoker = {
    if (args.length == 2) {
      new OnlyDirectInvoker {
        def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
          target == StartBenchmark && arguments.length == 2
        }

        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          val start = arguments(0).asInstanceOf[(Long, Long)]
          val iteration = arguments(1).asInstanceOf[Number]
          val end = Benchmark.getTimes()
          BenchmarkTimes(iteration.intValue(), nsToS(end._1 - start._1), nsToS(end._2 - start._2))
        }
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}