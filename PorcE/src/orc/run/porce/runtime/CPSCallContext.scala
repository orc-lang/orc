//
// CPSCallContext.scala -- Scala class CPSCallContext
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicBoolean

import orc.{ CallContext, CaughtEvent, OrcEvent }
import orc.compile.parse.OrcSourceRange
import orc.error.OrcException
import orc.run.porce.SimpleWorkStealingSchedulerWrapper

class CPSCallContext(val execution: PorcEExecution, val p: PorcEClosure, val c: Counter, val t: Terminator, val callSiteId: Int) extends AtomicBoolean with CallContext with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.

  val runtime = execution.runtime

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  def begin(): Unit = {
    Counter.flushAllCounterOffsets(flushOnlyPositive = true)
    t.addChild(this)
  }

  final def publishNonterminal(v: AnyRef): Unit = {
    /* ROOTNODE-STATISTICS
    p.body.getRootNode() match {
      case n: PorcERootNode => n.incrementPublication()
      case _ => ()
    }
    */
    c.newToken() // Token: Passed to p.
    val s = CallClosureSchedulable(p, v, execution)
    SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
    // Token: pass to p
    runtime.potentiallySchedule(s)
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override final def publish(v: AnyRef) = {
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (compareAndSet(false, true)) {
      /* ROOTNODE-STATISTICS
      p.body.getRootNode() match {
        case n: PorcERootNode => n.incrementPublication()
        case _ => ()
      }
      */
      val s = CallClosureSchedulable(p, v, execution)
      SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
      // Token: pass to p
      runtime.potentiallySchedule(s)
      t.removeChild(this)
    }
  }

  final def publishOptimized() = {
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (compareAndSet(false, true)) {
      /* ROOTNODE-STATISTICS
      p.body.getRootNode() match {
        case n: PorcERootNode => n.incrementPublication()
        case _ => ()
      }
      */
      t.removeChild(this)
      true
    } else {
      false
    }
  }

  final def kill(): Unit = halt()

  final def halt(): Unit = {
    if (compareAndSet(false, true)) {
      // Token: Taken from passed in c.
      c.haltToken()
      t.removeChild(this)
    }
  }

  final def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrcWithBoundary(event)
  }

  // TODO: Support VTime
  final def setQuiescent(): Unit = {}

  final def halt(e: OrcException): Unit = {
    notifyOrc(CaughtEvent(e))
    halt()
  }

  // TODO: Support rights.
  final def hasRight(rightName: String): Boolean = false

  final def isLive: Boolean = {
    t.isLive()
  }

  final def discorporate(): Unit = {
    if (compareAndSet(false, true)) {
      c.discorporateToken() // Token: Taken from passed in c.
      t.removeChild(this)
    }
  }

  // TODO: Add a mapping from callSiteIds to ranges or just nodes, then use that to look up the data we need here.
  def callSitePosition: Option[OrcSourceRange] = None

  override def toString() = {
    s"CPSCallContext@${hashCode().formatted("%x")}(${get()})"
  }
}
