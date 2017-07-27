//
// StandardOrcRuntime.scala -- Scala class StandardOrcRuntime
// Project OrcScala
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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

class PorcERuntime(engineInstanceName: String) extends Orc(engineInstanceName)
    with PorcEInvocationBehavior
    // with SupportForPorcEClosure
    with PorcEWithWorkStealingScheduler
    with SupportForRwait
    with SupportForSynchronousExecution
    with PorcERuntimeOperations 
    // with SupportForSchedulerUseProfiling 
    {

  override def removeRoot(arg: ExecutionRoot) = synchronized {
    super.removeRoot(arg)
    if (roots.isEmpty())
      stopScheduler()
  }
  def addRoot(root: ExecutionRoot) = roots.add(root)

  // TODO:PERFORMANCE: f will probably create an extra megamorphic call site. It may be better to have the caller create the CounterSchedulable instance.
  //    This decision should be made along with the decision of whether to actually perform direct calls here.
  def scheduleOrCall(c: Counter, f: () => Unit) = {
    if (PorcERuntime.checkAndImplementStackDepth()) {
      // Call in this stack      
      f()
    } else {
      // Schedule/trampoline
      schedule(new CounterSchedulableFunc(c, f))
    }
  }

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
    if(PorcERuntime.maxStackDepth > 0) {
      val depth = PorcERuntime.stackDepthThreadLocal.get()
      val r = depth < PorcERuntime.maxStackDepth
      if(r) 
        PorcERuntime.stackDepthThreadLocal.set(depth + 1)
      r
    } else {
      false
    }
  }
  
  def resetStackDepth() = {
    if(PorcERuntime.maxStackDepth > 0) {
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