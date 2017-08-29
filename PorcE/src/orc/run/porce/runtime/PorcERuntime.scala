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

/** The base runtime for PorcE runtimes.
 *  
 *  WARNING: This runtime does not support onSchedule and onComplete on 
 *  schedulables. See PorcEWithWorkStealingScheduler. 
  */
class PorcERuntime(engineInstanceName: String) extends Orc(engineInstanceName)
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

  @TruffleBoundary @noinline
  def spawn(c: Counter, computation: PorcEClosure): Unit = {
    schedule(CallClosureSchedulable(computation))
  }
}

object PorcERuntime {
  /*
  val stackDepthThreadLocal = new ThreadLocal[Int]() {
    override def initialValue() = {
      0
    }
  }

  def checkAndImplementStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      val depth = PorcERuntime.stackDepthThreadLocal.get()
      val r = depth < PorcERuntime.maxStackDepth
      if (r)
        PorcERuntime.stackDepthThreadLocal.set(depth + 1)
      r
    } else {
      false
    }
  }

  def resetStackDepth() = {
    if (PorcERuntime.maxStackDepth > 0) {
      PorcERuntime.stackDepthThreadLocal.set(0)
    }
  }

  val maxStackDepth = -1 // 24
  */
}
