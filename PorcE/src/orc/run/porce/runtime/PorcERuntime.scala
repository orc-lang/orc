//
// PorcERuntime.scala -- Scala class PorcERuntime
// Project PorcE
//
// Created by amp on Aug 03, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.{ ExecutionRoot, Schedulable }
import orc.run.Orc
import orc.run.extensions.{ SimpleWorkStealingScheduler, SupportForRwait, SupportForSynchronousExecution }
import orc.run.porce.{ PorcELanguage, SimpleWorkStealingSchedulerWrapper, SpecializationConfiguration }

import com.oracle.truffle.api.CompilerDirectives.{ CompilationFinal, TruffleBoundary }
import orc.run.porce.Logger
import java.util.logging.Level
import java.util.concurrent.atomic.LongAdder
import orc.util.DumperRegistry
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.CompilerDirectives.ValueType
import com.oracle.truffle.api.profiles.ConditionProfile
import orc.run.porce.profiles.VisibleConditionProfile

/** The base runtime for PorcE runtimes.
 *
 *  WARNING: This runtime does not support onSchedule and onComplete on
 *  schedulables. See PorcEWithWorkStealingScheduler.
  */
class PorcERuntime(engineInstanceName: String, val language: PorcELanguage) extends Orc(engineInstanceName)
  with PorcEInvocationBehavior
  with PorcEWithWorkStealingScheduler
  with SupportForRwait
  with SupportForSynchronousExecution
  // with SupportForSchedulerUseProfiling
  {

  override def removeRoot(arg: ExecutionRoot) = synchronized {
    super.removeRoot(arg)
    if (roots.isEmpty())
      stopScheduler()
  }
  def addRoot(root: ExecutionRoot) = roots.add(root)

  def beforeExecute(): Unit = {
    resetStackDepth()
  }

  def afterExecute(): Unit = {
  }

  val nonInlinableScheduleCount = new LongAdder()
  DumperRegistry.register(name => {
    val n = nonInlinableScheduleCount.sumThenReset()
    if (n > 1000)
      Logger.info(s"PERFORMANCE: $name nonInlinableScheduleCount=$n (This may indicate a performance problem.)")
    else if (n > 0)
      Logger.fine(s"PERFORMANCE: $name nonInlinableScheduleCount=$n")
  })

  @TruffleBoundary(allowInlining = false) @noinline
  def potentiallySchedule(s: Schedulable) = {
    if (logNoninlinableSchedules) {
      Logger.log(Level.WARNING, s"nonInlinableSchedule: $s", new Exception)
    }
    nonInlinableScheduleCount.increment()
    if (allowSpawnInlining || actuallySchedule) {
      def isFast = {
        s match {
          case s: CallClosureSchedulable =>
            s.closure.getTimePerCall() < SpecializationConfiguration.InlineAverageTimeLimit
        }
      }
      if (allowSpawnInlining && occationallySchedule && isFast) {
        val PorcERuntime.StackDepthState(b, prev) = incrementAndCheckStackDepth(null)
        if (b)
          try {
            val old = SimpleWorkStealingSchedulerWrapper.currentSchedulable
            SimpleWorkStealingSchedulerWrapper.enterSchedulable(s, SimpleWorkStealingSchedulerWrapper.StackExecution)
            s.run()
            SimpleWorkStealingSchedulerWrapper.exitSchedulable(s, old)
          } catch {
            case e: StackOverflowError =>
              // FIXME: Make this error fatal for the whole runtime and make sure the message describes how to fix it.
              throw e //new RuntimeException(s"Allowed stack depth too deep: ${stackDepthThreadLocal.get()}", e)
          } finally {
            decrementStackDepth(prev, null)
          }
      } else {
        //Logger.log(Level.INFO, s"Scheduling $s", new RuntimeException())
        schedule(s)
      }
    } else {
      val old = SimpleWorkStealingSchedulerWrapper.currentSchedulable
      SimpleWorkStealingSchedulerWrapper.enterSchedulable(s, SimpleWorkStealingSchedulerWrapper.StackExecution)
      s.run()
      SimpleWorkStealingSchedulerWrapper.exitSchedulable(s, old)
    }
  }

  private class IntHolder(var value: Int)
  // Only used on slow path when the thread is not a Worker.
  private val stackDepthThreadLocal = new ThreadLocal[IntHolder]() {
    override def initialValue() = {
      new IntHolder(0)
    }
  }

  def stackDepth: Int = {
    try {
      val t = Thread.currentThread.asInstanceOf[SimpleWorkStealingScheduler#Worker]
      t.stackDepth
    } catch {
      case _: ClassCastException => {
        if (allExecutionOnWorkers.isValid()) {
          CompilerDirectives.transferToInterpreterAndInvalidate()
          allExecutionOnWorkers.invalidate()
        }
        Int.MaxValue
      }
    }
  }

  @inline
  private def depthIncrement = if (CompilerDirectives.inCompiledCode()) 1 else 16

  @inline
  private final def incrementDepthValue(inlineAllowedProfile: VisibleConditionProfile, prev: Int): (Boolean, Int) = {
    if (unrollOnLargeStack) {
      val r = prev >= 0 &&
        (if (inlineAllowedProfile == null || !inlineAllowedProfile.wasFalse())
          prev < maxStackDepth
        else
          prev < minSpawnStackDepth)
      val next = prev + depthIncrement
      val b = inlineAllowedProfile != null && inlineAllowedProfile.profile(r) || inlineAllowedProfile == null && r
      (b, if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, b)) next else -1)
    } else {
      val r = if (inlineAllowedProfile == null || !inlineAllowedProfile.wasFalse()) prev < maxStackDepth else prev < minSpawnStackDepth
      val b = inlineAllowedProfile != null && inlineAllowedProfile.profile(r) || inlineAllowedProfile == null && r
      (b, prev + (if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, b)) depthIncrement else 0))
    }
  }

  /** Increment the stack depth and return true if we can continue to extend the stack.
    *
    * If this returns true the caller must call decrementStackDepth() after it finishes.
    */
  def incrementAndCheckStackDepth(inlineAllowedProfile: VisibleConditionProfile): PorcERuntime.StackDepthState = {
    if (maxStackDepth > 0) {
      @TruffleBoundary @noinline
      def incrementAndCheckStackDepthWithThreadLocal() = {
        val depth = stackDepthThreadLocal.get()
        val prev = depth.value
        val (r, n) = incrementDepthValue(inlineAllowedProfile, prev)
        depth.value = n
        PorcERuntime.StackDepthState(r, prev)
      }
      try {
        val t = Thread.currentThread.asInstanceOf[SimpleWorkStealingScheduler#Worker]
        val prev = t.stackDepth
        val (r, n) = incrementDepthValue(inlineAllowedProfile, prev)
        t.stackDepth = n
        PorcERuntime.StackDepthState(r, prev)
      } catch {
        case _: ClassCastException => {
          if (allExecutionOnWorkers.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate()
            allExecutionOnWorkers.invalidate()
          }
          incrementAndCheckStackDepthWithThreadLocal()
        }
      }
    } else {
      PorcERuntime.StackDepthState(false, 0)
    }
  }

  @inline
  private final def replaceDepthValue(curr: Int, prev: Int, unrollProfile: ConditionProfile): Int = {
    if (unrollOnLargeStack) {
      val b = curr < 0 && prev > 0
      if (unrollProfile != null && unrollProfile.profile(b) || unrollProfile == null && b) curr else prev
    } else {
      prev
    }
  }

  /** Decrement stack depth.
    *
    * @see incrementAndCheckStackDepth()
    */
  def decrementStackDepth(prev: Int, unrollProfile: ConditionProfile) = {
    if (maxStackDepth > 0) {
      @TruffleBoundary @noinline
      def decrementStackDepthWithThreadLocal() = {
        val depth = stackDepthThreadLocal.get()
        depth.value = replaceDepthValue(depth.value, prev, unrollProfile)
      }
      try {
        val t = Thread.currentThread.asInstanceOf[SimpleWorkStealingScheduler#Worker]
        t.stackDepth = replaceDepthValue(t.stackDepth, prev, unrollProfile)
      } catch {
        case _: ClassCastException => {
          if (allExecutionOnWorkers.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate()
            allExecutionOnWorkers.invalidate()
          }
          decrementStackDepthWithThreadLocal()
        }
      }
    }
  }

  def resetStackDepth() = {
    if (maxStackDepth > 0) {
      @TruffleBoundary @noinline
      def resetStackDepthWithThreadLocal() = {
        stackDepthThreadLocal.get().value = 0
      }
      try {
        val t = Thread.currentThread.asInstanceOf[SimpleWorkStealingScheduler#Worker]
        t.stackDepth = 0
      } catch {
        case _: ClassCastException => {
          if (allExecutionOnWorkers.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate()
            allExecutionOnWorkers.invalidate()
          }
          resetStackDepthWithThreadLocal()
        }
      }
    }
  }

  @inline
  @CompilationFinal
  final val logNoninlinableSchedules = System.getProperty("orc.porce.logNoninlinableSchedules", "false").toBoolean

  @inline
  @CompilationFinal
  final val allExecutionOnWorkers = Truffle.getRuntime.createAssumption("allExecutionOnWorkers")

  @inline
  @CompilationFinal
  final val minQueueSize = Option(System.getProperty("orc.porce.minQueueSize")).map(_.toInt).getOrElse(Runtime.getRuntime().availableProcessors() * 2)

  @inline
  @CompilationFinal
  final val maxStackDepth = System.getProperty("orc.porce.maxStackDepth", "16").toInt
  // TODO: Make maxStackDepth user configurable

  @inline
  @CompilationFinal
  final val minSpawnStackDepth = (maxStackDepth * 75) / 100

  @inline
  @CompilationFinal
  final val unrollOnLargeStack = System.getProperty("orc.porce.unrollOnLargeStack", "true").toBoolean

  @inline
  @CompilationFinal
  final val actuallySchedule = PorcERuntime.actuallySchedule

  @inline
  @CompilationFinal
  final val occationallySchedule = PorcERuntime.occationallySchedule

  @inline
  @CompilationFinal
  final val allowAllSpawnInlining = PorcERuntime.allowAllSpawnInlining

  @inline
  @CompilationFinal
  final val allowSpawnInlining = PorcERuntime.allowSpawnInlining
}

object PorcERuntime {
  @inline
  final val displayClosureValues = System.getProperty("orc.porce.displayClosureValues", "false").toBoolean

  @inline
  @CompilationFinal
  final val actuallySchedule = System.getProperty("orc.porce.actuallySchedule", "true").toBoolean

  @inline
  @CompilationFinal
  final val occationallySchedule = System.getProperty("orc.porce.occationallySchedule", "true").toBoolean

  @inline
  @CompilationFinal
  final val allowAllSpawnInlining = System.getProperty("orc.porce.allowAllSpawnInlining", "true").toBoolean

  @inline
  @CompilationFinal
  final val allowSpawnInlining = System.getProperty("orc.porce.allowSpawnInlining", "true").toBoolean

  @ValueType
  final case class StackDepthState(growthAllowed: Boolean, previousDepth: Int)

  // HACK: Force loading of a few classes in Truffle. Without this the error handling code crashes and destroys the stack trace.
  Option(Class.forName("com.oracle.truffle.api.TruffleStackTrace")).foreach(_.getClassLoader())
}
