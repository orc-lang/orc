package orc.run.porce.runtime

import orc.Handle
import java.util.concurrent.atomic.AtomicBoolean
import orc.OrcEvent
import orc.error.OrcException
import orc.CaughtEvent
import orc.compile.parse.OrcSourceRange
import orc.OrcRuntime

final class PCTHandle(val execution: PorcEExecution, val p: PorcEClosure, val c: Counter, val t: Terminator) extends Handle with Terminatable {
  val runtime = execution.runtime
  // TODO:PERFORMANCE: Use unsafe if this becomes a bottleneck.
  val halted = new AtomicBoolean(false)

  t.addChild(this)

  def publishNonterminal(v: AnyRef): Unit = {
    runtime.scheduleOrCall(c, () => p.callFromRuntime(v))
  }

  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    if (halted.compareAndSet(false, true)) {
      // TODO: It should be possible to pass the count we have on to the schedulable. It would save two atomic updates per pub. Only do if profiling shows this is an issue.
      runtime.scheduleOrCall(c, () => p.callFromRuntime(v))
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a
      //             prepareSpawn since it might spawn.
      t.removeChild(this)
    }
  }

  def kill(): Unit = halt()

  def halt(): Unit = {
    if (halted.compareAndSet(false, true)) {
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a
      //             prepareSpawn since it might spawn.
      t.removeChild(this)
    }
  }

  def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrc(event)
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
    if (halted.compareAndSet(false, true)) {
      c.discorporate()
      // Matched to: Every invocation is required to be proceeded by a
      //             prepareSpawn since it might spawn.
      t.removeChild(this)
    }
  }
}