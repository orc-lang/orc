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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

import orc.ExecutionRoot
import orc.run.Orc
import orc.run.extensions.{ SupportForRwait, SupportForSynchronousExecution }
//import orc.run.porce.Logger
import orc.run.porce.PorcELanguage
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import orc.Schedulable

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
    //PorcERuntime.resetStackDepth()
  }
  
  import PorcERuntime._
  
  def potentiallySchedule(s: Schedulable) = {
    if (actuallySchedule) {
      // TODO: Due to the memory retention caused by retained Counter/Terminator chains it may be useful to call this based on high-memory utilization as well. 
      if (occationallySchedule && incrementAndCheckStackDepth()) {
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
        //Logger.info(s"Scheduling (${stackDepthThreadLocal.get()}) $s")
        schedule(s)
      }
    } else {
      s.run()
    }
  }
}

object PorcERuntime {
  val stackDepthThreadLocal = new ThreadLocal[Int]() {
    override def initialValue() = {
      0
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
      /*if (depth > PorcERuntime.maxStackDepth / 2)
        Logger.fine(s"incr depth at $depth.")
        */
      val r = depth < PorcERuntime.maxStackDepth
      if (r)
        PorcERuntime.stackDepthThreadLocal.set(depth + 1)
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
      /*if (depth > PorcERuntime.maxStackDepth / 2)
        Logger.fine(s"decr depth at $depth.")
        */
      PorcERuntime.stackDepthThreadLocal.set(depth - 1)
    }
  }

  def resetStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      PorcERuntime.stackDepthThreadLocal.set(0)
    }
  }

  @inline
  @CompilationFinal
  val maxStackDepth = 32
  // TODO: Make maxStackDepth configurable. Any value >= 0 can theoretically cause a crash in a program that would otherwise have worked.
  
  @inline
  @CompilationFinal
  val actuallySchedule = true
  
  @inline
  @CompilationFinal
  val occationallySchedule = false

  
  // Force loading of a few classes in Truffle. Without this the error handling code crashes and destroys the stack trace.
  Class.forName("com.oracle.truffle.api.TruffleStackTrace").getFields()
}
