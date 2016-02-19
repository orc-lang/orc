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
import java.util.concurrent.atomic.AtomicBoolean

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
    isDone = true
  }

  final def installHandlers(h: EventHandler) = runtime.installHandlers(h)
  final def schedule(s: Schedulable) = runtime.schedule(s)
  final def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) = runtime.invoke(h, v, vs)
  
  if (Logger.julLogger.isLoggable(Level.FINE)) {
    val t = new Thread() {
      override def run(): Unit = {
        Thread.sleep(10*1000)
        Counter.report()
      }
    }
    t.setDaemon(true)
    t.setName("Counter Reporter")
    t.start()
  }
}

final class PCTHandle(execution: Execution, p: Continuation, c: Counter, t: Terminator, val callSitePosition: Position) extends Handle with Terminatable {
  val halted = new AtomicBoolean(false)
  
  t.addChild(this)
  
  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    if (halted.compareAndSet(false, true)) {
      execution.runtime.schedule(new CounterSchedulableFunc(c, () => {
        // Catch and ignore killed exceptions since the site call itself should not be killed.
        try {
          p.call(v)
        } catch {
          case _: KilledException => {}
        }
      }))
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a 
      //             prepareSpawn since it might spawn.
    }
  }

  def kill(): Unit = halt
  
  def halt: Unit = {
    if (halted.compareAndSet(false, true)) {
      c.halt()
      // Matched to: Every invocation is required to be proceeded by a 
      //             prepareSpawn since it might spawn.
    }
  }

  def notifyOrc(event: OrcEvent): Unit = {
    execution.notifyOrc(event)
  }

  // TODO: Support VTime
  def setQuiescent(): Unit = {}

  def !!(e: OrcException): Unit = {
    notifyOrc(CaughtEvent(e))
    halt
  }

  // TODO: Support rights.
  def hasRight(rightName: String): Boolean = false

  def isLive: Boolean = {
    t.isLive()
  }
}
