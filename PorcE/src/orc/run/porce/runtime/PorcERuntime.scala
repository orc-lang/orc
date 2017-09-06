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
import orc.run.porce.Logger
import orc.run.porce.PorcELanguage

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

  @TruffleBoundary(allowInlining = true) @noinline
  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    schedule(CallClosureSchedulable(computation))
  }
}

object PorcERuntime {
  val stackDepthThreadLocal = new ThreadLocal[Int]() {
    override def initialValue() = {
      0
    }
  }

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

  /*
  def resetStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      PorcERuntime.stackDepthThreadLocal.set(0)
    }
  }
  */

  @inline
  val maxStackDepth = 32
}
