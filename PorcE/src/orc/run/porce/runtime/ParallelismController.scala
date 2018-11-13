//
// ParallelismController.scala -- Scala class ParallelismController
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import scala.collection.JavaConverters._

import orc.run.porce.{ Logger, ParallelismNode, PorcERootNode, SpecializationConfiguration }

import com.oracle.truffle.api.nodes.NodeUtil
import java.util.{ TimerTask }
import java.lang.management.ManagementFactory
import orc.values.sites.TotalSite2Simple
import orc.values.Signal

object ReportPerformance extends TotalSite2Simple[Number, Number] {
  def eval(time: Number, reps: Number): Any = {
    ParallelismController.active.reportPerformance((time.doubleValue*1000000000).toLong, reps.longValue)
    Signal
  }
}

object ParallelismController {
  var _active: ParallelismController = null
  def active_=(v: ParallelismController) = {
    require(_active == null)
    _active = v
  }
  def active = _active
}

class ParallelismController(execution: PorcEExecution) {
  import orc.util.NumberFormatting._

  ParallelismController.active = this

  private def timer = execution.runtime.timer

  var reportedTime = 0L
  var reportedReps = 0L

  def reportPerformance(time: Long, reps: Long): Unit = synchronized {
    reportedTime += time
    reportedReps += reps
    scheduleCheck(true)
  }

  val parallelFractionValuesTable = Seq(1.0, 0.6, 0.3, 0.1, 0.01)
  var parallelFractionData = parallelFractionValuesTable.map((_, Time(0, 0, 0, 0, 0, 0)))
  var parallelFraction = 1.0

  private def timeMetric(t: Time) = {
    if (SpecializationConfiguration.UseRepTime) {
      t.timePerRep
    } else {
      -t.cpuUtilization
    }
  }
  private def timeMeasurementComplete(t: Time): Boolean = {
    if (SpecializationConfiguration.UseRepTime) {
      t.performanceReps >= 5 ||
      t.performanceTime > 2*1000000000L && t.performanceReps >= 3 ||
      t.performanceTime > 20*1000000000L
    } else {
      t.realTime > 20*1000000000L
    }
  }

  private def getParallelFractionTime(f: Double) = parallelFractionData.find(_._1 == f).get._2

  private def newCheckTask() = new TimerTask {
    override def run(): Unit = ParallelismController.this synchronized {
      checkTask = null
      check()
    }
  }
  private var checkTask: TimerTask = null
  private var lastTime: Time = null

  private def check(): Unit = synchronized {
    require(lastTime != null)
    if (checkTask != null) {
      return
    }

    val timeDiff = getTime() - lastTime
    val isWarn = lastTime.performanceReps >= 1
    val startTime = System.nanoTime()
    def timeSpent = s"(${(System.nanoTime() - startTime) / 1000000000.0 unit "s"})"

    // Only process roots that have run.
    lazy val rootsToUpdate = (Seq() ++ execution.allPorcERootNodes).filter(r => r.getTotalCalls > 0)

    // Check the profiling data and potentially perform control
    lazy val parallelismNodes = rootsToUpdate.flatMap(r => {
      val ps = NodeUtil.findAllNodeInstances(r, classOf[ParallelismNode]).asScala
      ps
    }).filter(_.isParallelismChoiceNode)
    lazy val totalCalls = rootsToUpdate.map(_.getTotalCalls).sum
    lazy val totalExecutionCount = parallelismNodes.map(_.getExecutionCount).sum

    def parallelFractionDataSorted = parallelFractionData.sortBy(p => timeMetric(p._2))

    Logger.info(f"Checking: totalExecutionCount=${totalExecutionCount unit " execs"}, totalCalls=${totalCalls unit " calls"}, timeDiff=$timeDiff $timeSpent")
    lastTime = getTime()

    if (isWarn ||
        totalExecutionCount > SpecializationConfiguration.MinimumExecutionCountForParallelismController ||
        totalCalls > SpecializationConfiguration.MinimumExecutionCountForParallelismController*100) {
      // Once we get here we are ready to do real updates.

      if (timeDiff.compilationTime / timeDiff.cpuTime < 0.1) {
        // If we used less than 10% of the time for compilation then add this chunk to the appropriate data
        parallelFractionData = parallelFractionData map {
          case (f, old) if f == parallelFraction => (f, old + timeDiff)
          case x => x
        }
        Logger.info(s"Added to $parallelFraction. parallelFractionData:\n" +
            parallelFractionDataSorted.mkString("\n"))
      }

      // Find the first parallelFraction without enough data if any.
      val (newParallelFraction, stillCollecting) = parallelFractionData.find(p => !timeMeasurementComplete(p._2)) match {
          case Some((fract, _)) =>
            // We are still collecting data.
            (fract, true)
          case None =>
            // We have all our data. Pick the best.
            // TODO: If we are allowed to use the actual run time of the operation we could use that here.
            (parallelFractionDataSorted.head._1, false)
      }

      val metricDiff =
        timeMetric(getParallelFractionTime(newParallelFraction)) - timeMetric(getParallelFractionTime(parallelFraction))
      if (metricDiff.abs > 0.1 || stillCollecting) {
        parallelFraction = newParallelFraction
        Logger.info(s"Switching to $newParallelFraction, parallelFractionData:\n" +
            parallelFractionDataSorted.mkString("\n"))
      }

      // Build node lists and find prefix length which has parallelFraction executions
      val sortedNodes = parallelismNodes.sortBy(_.getExecutionCount)
      val targetCount = (totalExecutionCount * parallelFraction).toLong
      val prefixLen = sortedNodes.scanLeft(0L)(_ + _.getExecutionCount).indexWhere(_ > targetCount)

      // Disable profiling in roots have already dealth with.
      for (r <- rootsToUpdate) {
        r.setProfiling(false)
      }

      // Set the initial prefixLen nodes to be parallel and all others not to be
      for ((n, i) <- sortedNodes.zipWithIndex) {
        n.setParallel(i < prefixLen)
      }

      Logger.info(s"new parallel ExecutionCount=${sortedNodes.take(prefixLen).map(_.getExecutionCount).sum unit " execs"}, n roots = ${rootsToUpdate.size}, n parallel=${prefixLen unit " nodes"}, n nodes=${sortedNodes.size unit " nodes"} $timeSpent\n" +
          f"parallelFraction=$parallelFraction%.3f (${targetCount unit " execs"}), timeDiff=$timeDiff")
      Logger.finer({
        val sb = new StringBuffer()
        sb.append("\n")
        for ((n, i) <- sortedNodes.zipWithIndex) {
          sb.append(s"${if (i < prefixLen) "*" else " "} ${n.getExecutionCount unit " execs"} ${n.getRootNode().asInstanceOf[PorcERootNode].getTotalCalls unit " calls"}\t| $n\n")
        }
        sb.toString
      })
    } else {
      // If it's not time yet, schedule a check
      scheduleCheck()
    }
  }

  private def scheduleCheck(now: Boolean = false) = {
    if (lastTime == null) {
      lastTime = getTime()
    }
    if (checkTask == null) {
      checkTask = newCheckTask()
      timer.schedule(checkTask, if (now) 100 else 10 /* s */ * 1000)
    }
  }

  val osmxbean = ManagementFactory.getOperatingSystemMXBean() match {
    case v: com.sun.management.OperatingSystemMXBean => v
    case _ => throw new AssertionError("ParallelismController requires com.sun.management.OperatingSystemMXBean")
  }
  val compilermxbean = ManagementFactory.getCompilationMXBean()
  val gcmxbean = ManagementFactory.getGarbageCollectorMXBeans().asScala

  def getGCTime() = gcmxbean.map(_.getCollectionTime()).sum * 1000000
  def getCompilationTime() = compilermxbean.getTotalCompilationTime() * 1000000
  def getCPUTime() = osmxbean.getProcessCpuTime()
  def getRealTime() = System.nanoTime()

  case class Time(realTime: Long, cpuTime: Long,
      compilationTime: Long, gcTime: Long,
      performanceTime: Long, performanceReps: Long) {
    def cpuUtilization = (cpuTime - compilationTime - gcTime).toDouble / realTime
    def machineUtilization = cpuUtilization / osmxbean.getAvailableProcessors
    def timePerRep = performanceTime.toDouble / performanceReps

    def -(o: Time) = Time(realTime - o.realTime, cpuTime - o.cpuTime,
        compilationTime - o.compilationTime, gcTime - o.gcTime,
        performanceTime - o.performanceTime, performanceReps - o.performanceReps)
    def +(o: Time) = Time(realTime + o.realTime, cpuTime + o.cpuTime,
        compilationTime + o.compilationTime, gcTime + o.gcTime,
        performanceTime + o.performanceTime, performanceReps + o.performanceReps)

    override def toString() =
      f"Time(${realTime/1000000000.0 unit "s"}, ${cpuTime/1000000000.0 unit "s"}, " +
      f"${compilationTime/1000000000.0 unit "s"},  ${gcTime/1000000000.0 unit "s"}, " +
      f"${performanceTime/1000000000.0 unit "s"}, ${performanceReps unit ""}, " +
      f"[$machineUtilization%.3f, ${timePerRep/1000000000.0 unit "s"}])"
  }

  def getTime() = Time(getRealTime(), getCPUTime(),
      getCompilationTime(), getGCTime(),
      reportedTime, reportedReps)


  // Schedule initial check.
  scheduleCheck()
}

