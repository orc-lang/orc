package orc.run.porce.runtime

import orc.Handle
import java.util.concurrent.atomic.AtomicBoolean
import orc.OrcEvent
import orc.error.OrcException
import orc.CaughtEvent
import orc.compile.parse.OrcSourceRange
import orc.OrcRuntime
import orc.run.porce.distrib.DOrcMarshalingReplacement

class CPSCallResponseHandler(val execution: PorcEExecution, val p: PorcEClosure, val c: Counter, val t: Terminator, val callSiteId: Int) extends AtomicBoolean with Handle with Terminatable {
  // The value stored in the AtomicBoolean is a flag saying if we have already halted.

  val runtime = execution.runtime

  t.addChild(this)

  final def publishNonterminal(v: AnyRef): Unit = {
    c.newToken() // Token: Passed to p.
    runtime.schedule(CallClosureSchedulable(p, v))
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override final def publish(v: AnyRef) = {
    // This is an optimization of publishNonterminal then halt. We pass the token directly to p instead of creating a new one and then halting it.
    if (compareAndSet(false, true)) {
      // Token: Pass token to p.
      runtime.schedule(CallClosureSchedulable(p, v))
      t.removeChild(this)
    }
  }

  final def kill(): Unit = halt()

  final def halt(): Unit = {
    if (compareAndSet(false, true)) {
      c.haltToken() // Token: Taken from passed in c.
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

  def callRecord = new CallRecord(p, c, t, callSitePosition, callSiteId)

  override def toString() = {
    s"CPSCallResponseHandler@${hashCode().formatted("%x")}(${get()})"
  }
}

class CallRecord private (p_ : AnyRef, c_ : AnyRef, t_ : AnyRef, val callSitePosition: Option[OrcSourceRange], val callSiteId: Int) extends DOrcMarshalingReplacement with Serializable {
  def this(p: PorcEClosure, c: Counter, t: Terminator, callSitePosition: Option[OrcSourceRange], callSiteId: Int) = {
    this(p: AnyRef, c: AnyRef, t: AnyRef, callSitePosition, callSiteId)
  }

  override def isReplacementNeededForMarshaling(marshalValueWouldReplace: AnyRef => Boolean): Boolean = {
    marshalValueWouldReplace(p_) || marshalValueWouldReplace(c_) || marshalValueWouldReplace(t_)
  }

  override def replaceForMarshaling(marshaler: AnyRef => AnyRef): AnyRef = {
    new CallRecord(marshaler(p_), marshaler(c_), marshaler(t_), callSitePosition, callSiteId)
  }

  override def isReplacementNeededForUnmarshaling(unmarshalValueWouldReplace: AnyRef => Boolean): Boolean = {
    unmarshalValueWouldReplace(p_) || unmarshalValueWouldReplace(c_) || unmarshalValueWouldReplace(t_)
  }

  override def replaceForUnmarshaling(unmarshaler: AnyRef => AnyRef) = {
    new CallRecord(unmarshaler(p_), unmarshaler(c_), unmarshaler(t_), callSitePosition, callSiteId)
  }

  def p: PorcEClosure = p_.asInstanceOf[PorcEClosure]
  def c: Counter = c_.asInstanceOf[Counter]
  def t: Terminator = t_.asInstanceOf[Terminator]
}