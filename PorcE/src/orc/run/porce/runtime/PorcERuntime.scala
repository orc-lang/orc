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

import orc.ExecutionRoot
import orc.run.Orc
import orc.run.StandardInvocationBehavior
import orc.run.extensions.OrcWithWorkStealingScheduler
import orc.run.extensions.SupportForRwait
import orc.run.extensions.SupportForSynchronousExecution
import orc.run.extensions.SupportForSchedulerUseProfiling
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

class PorcERuntime(engineInstanceName: String) extends Orc(engineInstanceName)
  with PorcEInvocationBehavior
  // with SupportForPorcEClosure
  with PorcEWithWorkStealingScheduler
  with SupportForRwait
  with SupportForSynchronousExecution
  with PorcERuntimeOperations // with SupportForSchedulerUseProfiling 
  {

  override def removeRoot(arg: ExecutionRoot) = synchronized {
    super.removeRoot(arg)
    if (roots.isEmpty())
      stopScheduler()
  }
  def addRoot(root: ExecutionRoot) = roots.add(root)

  def beforeExecute(): Unit = {
    PorcERuntime.resetStackDepth()
  }
}

object PorcERuntime {
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
}

/*
trait SupportForPorcEClosure extends InvocationBehavior {
  abstract override def getInvoker(target: AnyRef, arguments: Array[AnyRef]): Invoker = {
    
  }
  
  abstract override def getAccessor(target: AnyRef, field: Field): Accessor = {
  }
}
*/ 