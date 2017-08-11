package orc.run.porce.runtime

import orc.Handle
import java.util.concurrent.atomic.AtomicBoolean
import orc.OrcEvent
import orc.error.OrcException
import orc.CaughtEvent
import orc.compile.parse.OrcSourceRange
import orc.OrcRuntime

final class CPSCallResponseHandler(val execution: PorcEExecution, val p: PorcEClosure, val c: Counter, val t: Terminator) extends AtomicBoolean with Handle with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.

  val runtime = execution.runtime

  t.addChild(this)

  def publishNonterminal(v: AnyRef): Unit = {
    c.newToken() // Token: Passed to p.
    runtime.schedule(CallClosureSchedulable(p, v))
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (compareAndSet(false, true)) {
      // Token: Pass token to p.
      runtime.schedule(CallClosureSchedulable(p, v))
      t.removeChild(this)
    }
  }

  def kill(): Unit = halt()

  def halt(): Unit = {
    if (compareAndSet(false, true)) {
      c.haltToken() // Token: Taken from passed in c.
      t.removeChild(this)
    }
  }

  def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrcWithBoundary(event)
  }

  // TODO: Support VTime
  def setQuiescent(): Unit = {}

  def halt(e: OrcException): Unit = {
    notifyOrc(CaughtEvent(e))
    halt()
  }

  // TODO: Support rights.
  def hasRight(rightName: String): Boolean = false

  def isLive: Boolean = {
    t.isLive()
  }

  // TODO: Get information from calling PorcE node using stack introspection in truffle.
  def callSitePosition: Option[OrcSourceRange] = None

  def discorporate(): Unit = {
    if (compareAndSet(false, true)) {
      c.discorporateToken() // Token: Taken from passed in c.
      t.removeChild(this)
    }
  }
  
  override def toString() = {
    s"CPSCallResponseHandler@${hashCode().formatted("%x")}(${get()})"
  }
}