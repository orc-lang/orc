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
import orc.MaterializedCallContext
import java.util.ArrayList
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

sealed abstract class CallContextCommon(execution: PorcEExecution, callSiteId: Int) extends CallContext {
  def t: Terminator

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

  // TODO: Add a mapping from callSiteIds to ranges or just nodes, then use that to look up the data we need here.
  def callSitePosition: Option[OrcSourceRange] = None
}

abstract class VirtualCallContextBase(val execution: PorcEExecution, val callSiteId: Int)
    extends CallContextCommon(execution, callSiteId) {
  def p: PorcEClosure
  def c: Counter
  def t: Terminator

  protected def isClean: Boolean

  var isOpen = true

  def close(): Unit = {
    isOpen = false
  }

  def materialize(): MaterializedCallContext = {
    assert(isClean, s"$this cannot be materialized because it is not clean.")
    val r = new CPSCallContext(execution, p, c, t, callSiteId)
    close()
    r.begin()
    r
  }

  def runtime = execution.runtime

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  final def discorporate(): Unit = {
    c.setDiscorporate()
    halt()
  }
}

object DirectVirtualCallContext {
  /** The size in *items* of each buffer when it is first used.
    */
  @inline
  private val initialBufferSize = 2
}

abstract class DirectVirtualCallContext(execution: PorcEExecution, callSiteId: Int)
    extends VirtualCallContextBase(execution, callSiteId) {
  virtualCtx =>

  var halted: Boolean = false
  var selfPublicationList: ArrayList[AnyRef] = null
  var otherPublicationList: ArrayList[AnyRef] = null
  var otherHaltedList: ArrayList[CPSCallContext] = null

  protected def isClean = isOpen && halted == false && selfPublicationList == null && otherPublicationList == null

  override def close() = {
    super.close()
    selfPublicationList = null
    otherPublicationList = null
    otherHaltedList = null
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private def addSelfPublication(v: AnyRef) = {
    assert(isOpen)
    if (selfPublicationList == null) {
      selfPublicationList = new ArrayList(DirectVirtualCallContext.initialBufferSize)
    }
    selfPublicationList.add(v)
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private def addOtherPublication(o: CPSCallContext, v: AnyRef, halt: Boolean) = {
    assert(isOpen)
    if (otherPublicationList == null) {
      // Each other publication is 3 array elements.
      otherPublicationList = new ArrayList(DirectVirtualCallContext.initialBufferSize * 3)
    }
    otherPublicationList.add(o)
    otherPublicationList.add(v)
    otherPublicationList.add(halt.asInstanceOf[AnyRef])
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private def addOtherHalt(o: CPSCallContext) = {
    assert(isOpen)
    if (otherHaltedList == null) {
      otherHaltedList = new ArrayList(DirectVirtualCallContext.initialBufferSize)
    }
    otherHaltedList.add(o)
  }

  def virtualCallContextFor(ctx: CallContext): CallContext = {
    assert(isOpen)
    if (true)
    ctx match {
      case ctx: CPSCallContext => new VirtualCallContextBase(execution, callSiteId) {
        def p = ctx.p
        def c = ctx.c
        def t = ctx.t

        protected def isClean = virtualCtx.isClean

        override def materialize(): MaterializedCallContext = {
          assert(isClean, s"$this cannot be materialized because it is not clean.")
          close()
          ctx
        }

        def virtualCallContextFor(ctx: CallContext): CallContext = {
          virtualCtx.virtualCallContextFor(ctx)
        }

        final def publishNonterminal(v: AnyRef): Unit = {
          /* ROOTNODE-STATISTICS
          p.body.getRootNode() match {
            case n: PorcERootNode => n.incrementPublication()
            case _ => ()
          }
          */
          virtualCtx.addOtherPublication(ctx, v, false)
        }

        /** Handle a site call publication.
          *
          * The semantics of a publication for a handle include halting so add that.
          */
        override final def publish(v: AnyRef) = {
          assert(isOpen)
          // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
          if (ctx.halted.compareAndSet(false, true)) {
            /* ROOTNODE-STATISTICS
            p.body.getRootNode() match {
              case n: PorcERootNode => n.incrementPublication()
              case _ => ()
            }
            */
            virtualCtx.addOtherPublication(ctx, v, true)
          }
        }

        final def halt(): Unit = {
          assert(isOpen)
          if (ctx.halted.compareAndSet(false, true)) {
            virtualCtx.addOtherHalt(ctx)
          }
        }

        override def toString() = {
          s"VirtualCallContext(for ${ctx}, in ${virtualCtx})"
        }
      }
      case ctx: VirtualCallContextBase => ctx
      case ctx => ctx
    }
    else
    ctx
  }

  final def publishNonterminal(v: AnyRef): Unit = {
    /* ROOTNODE-STATISTICS
    p.body.getRootNode() match {
      case n: PorcERootNode => n.incrementPublication()
      case _ => ()
    }
    */
    addSelfPublication(v)
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override final def publish(v: AnyRef) = {
    assert(isOpen)
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (!halted) {
      /* ROOTNODE-STATISTICS
      p.body.getRootNode() match {
        case n: PorcERootNode => n.incrementPublication()
        case _ => ()
      }
      */
      addSelfPublication(v)
      halt()
    }
  }

  final def halt(): Unit = {
    halted = true
  }

  override def toString() = {
    s"VirtualCallContext@${hashCode().formatted("%x")}($halted, $selfPublicationList, $otherPublicationList, $otherHaltedList)"
  }
}


class CPSCallContext(val execution: PorcEExecution, val p: PorcEClosure, val c: Counter, val t: Terminator, val callSiteId: Int)
    extends CallContextCommon(execution, callSiteId) with MaterializedCallContext with Terminatable {
  // A flag saying if we have already halted.
  val halted = new AtomicBoolean(false)

  val runtime = execution.runtime

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  def begin(): Unit = {
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
    if (halted.compareAndSet(false, true)) {
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
    if (halted.compareAndSet(false, true)) {
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
    s"CPSCallContext@${hashCode().formatted("%x")}(${halted.get()})"
  }
}
