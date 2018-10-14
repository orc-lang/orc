//
// MaterializedCPSCallContext.scala -- Scala class MaterializedCPSCallContext
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

import orc.{ CallContext, CaughtEvent, MaterializedCallContext, OrcEvent, SiteResponseSet, VirtualCallContext }
import orc.compile.parse.OrcSourceRange
import orc.error.OrcException
import orc.run.porce.SimpleWorkStealingSchedulerWrapper

sealed abstract class CallContextCommon(
    val execution: PorcEExecution,
    val p: PorcEClosure, val c: Counter, val t: Terminator,
    val callSiteId: Int) extends CallContext {

  final def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrcWithBoundary(event)
  }

  // TODO: Support rights.
  final def hasRight(rightName: String): Boolean = false

  final def isLive: Boolean = {
    t.isLive()
  }

  // TODO: Add a mapping from callSiteIds to ranges or just nodes, then use that to look up the data we need here.
  def callSitePosition: Option[OrcSourceRange] = None

  def runtime = execution.runtime
}

class VirtualCPSCallContext(_execution: PorcEExecution, _p: PorcEClosure, _c: Counter, _t: Terminator, _callSiteId: Int)
    extends CallContextCommon(_execution, _p, _c, _t, _callSiteId) with VirtualCallContext {

  /** Create an empty SiteResponseSet.
    *
    * This ResultDescriptor is optimized for receiving some unknown number of
    * publications and other actions.
    */
  def empty(): SiteResponseSet = ResponseSet.Empty

  def materialize(): MaterializedCallContext = {
    val r = new MaterializedCPSCallContext(execution, p, c, t, callSiteId)
    r
  }

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)
}

sealed abstract class ResponseSet(val next: ResponseSet) extends SiteResponseSet {
  protected[this] def selfAsNext: ResponseSet = this

  /** Publish a value from ctx without halting the call.
    */
  override def publishNonterminal(ctx: CallContext, v: AnyRef): ResponseSet =
    new ResponseSet.PublishNonterminal(ctx, v, selfAsNext)

  /** Publish a value from ctx and halt ctx.
    */
  override def publish(ctx: CallContext, v: AnyRef): ResponseSet =
    new ResponseSet.PublishTerminal(ctx, v, selfAsNext)

  /** Halt ctx without publishing a value.
    */
  override def halt(ctx: CallContext): ResponseSet =
    this.halt(ctx, null)

  /** Halt ctx without publishing a value, providing an exception which caused the halt.
    */
  override def halt(ctx: CallContext, e: OrcException): ResponseSet =
    new ResponseSet.Halt(ctx, e, selfAsNext)

  /** Notify the runtime that ctx will never publish again, but will not halt.
    */
  override def discorporate(ctx: CallContext): ResponseSet =
    new ResponseSet.Discorporate(ctx, selfAsNext)
}

object ResponseSet {
  object Empty extends ResponseSet(null) {
    protected[this] override def selfAsNext = null
  }
  final class PublishNonterminal(val ctx: CallContext, val v: AnyRef, _next: ResponseSet) extends ResponseSet(_next) {
    override def halt(ctx: CallContext): ResponseSet =
      new PublishTerminal(ctx, v, next)
  }
  final class PublishTerminal(val ctx: CallContext, val v: AnyRef, _next: ResponseSet) extends ResponseSet(_next)
  final class Halt(val ctx: CallContext, val e: OrcException, _next: ResponseSet) extends ResponseSet(_next)
  final class Discorporate(val ctx: CallContext, _next: ResponseSet) extends ResponseSet(_next)
}

class MaterializedCPSCallContext(_execution: PorcEExecution, _p: PorcEClosure, _c: Counter, _t: Terminator, _callSiteId: Int)
    extends CallContextCommon(_execution, _p, _c, _t, _callSiteId) with MaterializedCallContext with Terminatable {
  // A flag saying if we have already halted.
  val halted = new AtomicBoolean(false)

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  // Flush positive counter because p may execute in another thread.
  Counter.flushAllCounterOffsets(1)

  t.addChild(this)

  final def publishNonterminal(v: AnyRef): Unit = {
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
    if (halted.compareAndSet(false, true)) {
      val s = CallClosureSchedulable(p, v, execution)
      SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
      // Token: pass to p
      runtime.potentiallySchedule(s)
      t.removeChild(this)
    }
  }

  final def publishOptimized() = {
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (halted.compareAndSet(false, true)) {
      t.removeChild(this)
      true
    } else {
      false
    }
  }

  final def kill(): Unit = halt()

  // TODO: Support VTime
  final def setQuiescent(): Unit = {}

  final def halt(e: OrcException): Unit = {
    notifyOrc(CaughtEvent(e))
    halt()
  }

  final def halt(): Unit = {
    if (halted.compareAndSet(false, true)) {
      // Token: Taken from passed in c.
      c.haltToken()
      t.removeChild(this)
    }
  }

  final def discorporate(): Unit = {
    if (halted.compareAndSet(false, true)) {
      c.discorporateToken() // Token: Taken from passed in c.
      t.removeChild(this)
    }
  }

  override def toString() = {
    s"MaterializedCPSCallContext@${hashCode().formatted("%08x")}(${halted.get()})"
  }
}
