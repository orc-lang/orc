//
// PorcERuntime.scala -- Scala class PorcERuntime
// Project PorcE
//
// Created by amp on Aug 03, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.logging.Level

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

import orc.ExecutionRoot
import orc.run.Orc
import orc.run.extensions.{ SupportForRwait, SupportForSynchronousExecution }
import orc.run.porce.Logger
import orc.run.porce.PorcELanguage
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import orc.Schedulable
import orc.run.porce.SpecializationConfiguration
import com.oracle.truffle.api.CompilerDirectives
import orc.run.porce.PorcERootNode
import java.util.concurrent.atomic.LongAdder

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

	private val spawnCounter = new LongAdder();
  
  def spawnCount = spawnCounter.sum()

  def beforeExecute(): Unit = {
    //PorcERuntime.resetStackDepth()
    spawnCounter.increment()
  }
  
  import PorcERuntime._
  
  def potentiallySchedule(s: Schedulable) = {
    if (actuallySchedule) {
      // TODO: Due to the memory retention caused by retained Counter/Terminator chains it may be useful to call this based on high-memory utilization as well.
      def isFast = {
        s match {
          case s: CallClosureSchedulable =>
            s.closure.getTimePerCall() < SpecializationConfiguration.InlineAverageTimeLimit
        }
      }
      if (occationallySchedule &&
          isFast &&
          incrementAndCheckStackDepth()) {
        try {
          s.run()
        } catch {
          case e: StackOverflowError =>
            // FIXME: Make this error fatal for the whole runtime and make sure the message describes how to fix it.
            throw e //new RuntimeException(s"Allowed stack depth too deep: ${stackDepthThreadLocal.get()}", e)
        } finally {
          decrementStackDepth()
        }
      } else {
        //Logger.log(Level.INFO, s"Scheduling $s", new RuntimeException())
  			if (CompilerDirectives.inInterpreter()) {
  			  s match {
  			    case s: CallClosureSchedulable =>
  			      s.closure.body.getRootNode match {
  			        case r: PorcERootNode => r.incrementSpawn()
  			        case _ => ()
  			      }
  			  }
  				//Logger.info(() -> "Spawning call: " + computation + ", body =  " + computation.body.getRootNode() + " (" + computation.body.getRootNode().getClass() + "), getTimePerCall() = " + computation.getTimePerCall());
  			}
        schedule(s)
      }
    } else {
      s.run()
    }
  }
}

object PorcERuntime {
  private class IntHolder(var value: Int)
  private val stackDepthThreadLocal = new ThreadLocal[IntHolder]() {
    override def initialValue() = {
      new IntHolder(0)
    }
  }

  /** Increment the stack depth and return true if we can continue to extend the stack.
    * 
    * If this returns true the caller must call decrementStackDepth() after it finishes.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def incrementAndCheckStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      val depth = PorcERuntime.stackDepthThreadLocal.get()
      //if (depth.value > PorcERuntime.maxStackDepth / 2)
      //  Logger.log(Level.INFO, s"incr (depth=${depth.value})")
        
      val r = depth.value < PorcERuntime.maxStackDepth
      if (r)
        depth.value += 1
      r
    } else {
      false
    }
  }

  /** Decrement stack depth.
    *
    * @see incrementAndCheckStackDepth()
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def decrementStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      val depth = PorcERuntime.stackDepthThreadLocal.get()
      //if (depth.value > PorcERuntime.maxStackDepth / 2)
      //  Logger.log(Level.INFO, s"decr (depth=${depth.value})")
        
      depth.value -= 1
    }
  }

  def resetStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      PorcERuntime.stackDepthThreadLocal.get().value = 0
    }
  }

  @inline
  @CompilationFinal
  val maxStackDepth = System.getProperty("orc.porce.maxStackDepth", "8").toInt
  // TODO: Make maxStackDepth user configurable
  
  @inline
  @CompilationFinal
  val actuallySchedule = System.getProperty("orc.porce.actuallySchedule", "true").toBoolean
  
  @inline
  @CompilationFinal
  val occationallySchedule = System.getProperty("orc.porce.occationallySchedule", "true").toBoolean
  
  @inline
  @CompilationFinal
  val allowAllSpawnInlining = System.getProperty("orc.porce.allowAllSpawnInlining", "false").toBoolean
  
  @inline
  @CompilationFinal
  val allowSpawnInlining = System.getProperty("orc.porce.allowSpawnInlining", "true").toBoolean

  
  // HACK: Force loading of a few classes in Truffle. Without this the error handling code crashes and destroys the stack trace.
  Option(Class.forName("com.oracle.truffle.api.TruffleStackTrace")).foreach(_.getClassLoader())
}
