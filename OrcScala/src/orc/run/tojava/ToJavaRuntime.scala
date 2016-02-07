package orc.run.tojava

import orc.OrcRuntime
import java.util.function.Consumer
import orc.values.OrcRecord
import orc.values.Field
import scala.util.parsing.input.Position
import orc.Handle
import orc.{ CaughtEvent, Handle, OrcEvent, OrcRuntime }
import orc.error.OrcException
import orc.lib.str.PrintEvent
import orc.run.Logger
import orc.run.extensions.RwaitEvent
import java.util.TimerTask
import java.util.Timer
import java.util.logging.Level
import orc.run.core.EventHandler
import orc.run.Orc
import orc.Schedulable

/** @author amp
  */
class ToJavaRuntime(private val runtime: Orc) {
  // TODO: ToJavaRuntime should really be a runtime not have a runtime. For now I'm avoiding abstracting and duplicating code from the other interpreter.
  private[this] var executions = Set[Execution]()
  private[this] var isDone = false

  def addExecution(arg: Execution) = synchronized {
    assert(!isDone)
    executions += arg
  }

  def removeExecution(arg: Execution) = synchronized {
    executions -= arg
    if (executions.isEmpty)
      shutdown()
  }

  def shutdown() = synchronized {
    runtime.stopScheduler()
    timer.cancel()
    isDone = true
  }

  /** A timer instance to implement Rwait events.
    */
  val timer: Timer = new Timer()

  final def installHandlers(h: EventHandler) = runtime.installHandlers(h)
  final def schedule(s: Schedulable) = runtime.schedule(s)
  final def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) = runtime.invoke(h, v, vs)
}

/** An implementation of site call Handle which is also a tojava Context.
  */
final class PCTHandle(execution: Execution, p: Continuation, c: Counter, t: Terminator, val callSitePosition: Position) extends Handle {
  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    // Catch and ignore killed exceptions since the site call itself should not be killed.
    try {
      p.call(v)
    } catch {
      case _: KilledException => {}
    }
    c.halt()
    // Matched to: Every invocation is required to be proceeded by a 
    //             prepareSpawn since it might spawn.
  }

  def halt: Unit = {
    c.halt()
  }

  def notifyOrc(event: OrcEvent): Unit = execution.notifyOrc(event)

  // TODO: Support VTime
  def setQuiescent(): Unit = {}

  /** Print a warning and halt if there is an exception.
    */
  def !!(e: OrcException): Unit = {
    Logger.log(Level.WARNING, "Exception in execution:", e)
    c.halt()
    // Matched to: Every invocation is required to be proceeded by a 
    //             prepareSpawn since it might spawn.
  }

  // TODO: Support rights.
  def hasRight(rightName: String): Boolean = false

  def isLive: Boolean = {
    t.isLive()
  }
}

object PCTHandle {
}
