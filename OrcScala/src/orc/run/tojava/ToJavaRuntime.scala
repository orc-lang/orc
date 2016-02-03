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

/**
 * @author amp
 */
class ToJavaRuntime(val runtime: OrcRuntime) {  
  /** Spawn another execution.
    *
    * The odd choice of argument type is so that Java 8 code can call into it
    * easily.
    */
  def spawn(c: Counter, t: Terminator, f: Runnable): Unit = {
    // Spawn is an expensive operation, so check for kill before we do it.
    t.checkLive();
    // Schedule the work. prepareSpawn and halt are called by
    // ContextSchedulableFunc.
    runtime.schedule(new CounterSchedulableRunnable(c, f))
  }

  /** Spawn an execution and return future for it's first publication.
    *
    * This is very similar to spawn().
    */
  def spawnFuture(c: Counter, t: Terminator, f: Consumer[Continuation]): Future = {
    t.checkLive();
    val fut = new Future(runtime)
    runtime.schedule(new CounterSchedulableFunc(c, () =>
      f.accept(new Continuation {
        // This special context just binds the future on publication.
        override def call(v: AnyRef): Unit = {
          fut.bind(v)
        }
      })))
    fut
  }

  /** Force a value if it is a future.
    */
  def force(p: Continuation, c: Counter, v: AnyRef) = {
    v match {
      case f: Future =>
        f.forceIn(new PCBlockable(p, c))
      case _ => p.call(v)
    }
  }

  /** Get a field of an object if possible.
    *
    * This may result in a site call and hence a spawn. However all the halts
    * and stuff are handled internally. However it does mean that this
    * returning does not imply this has halted.
    */
  def getField(p: Continuation, c: Counter, t: Terminator, v: AnyRef, f: Field) = {
    v match {
      case r: OrcRecord => {
        // This just optimizes the record case.
        r.entries.get(f.field) match {
          case Some(w) => p.call(v)
          case None => {}
        }
      }
      case _ => {
        // Use the old style call with field to get the value.
        val callable = Coercions.coerceToCallable(v)
        callable.call(this, p, c, t, Array[AnyRef](f))
      }
    }
  }
}

/** An implementation of site call Handle which is also a tojava Context.
  */
final class PCTHandle(p: Continuation, c: Counter, t: Terminator, val callSitePosition: Position) extends Handle {
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

  /** Handle an OrcEvent from a call.
    *
    * This is done in place instead of passing it up to the execution like
    * in the Token interpreter.
    */
  def notifyOrc(event: OrcEvent): Unit = {
    // TODO: This should be replaced with a call to runtime or similar.
    // This implementation is not extensible.
    event match {
      case CaughtEvent(e) => e.printStackTrace()
      case PrintEvent(s) => print(s)
      case RwaitEvent(delay, h) => {
        val callback =
          new TimerTask() {
            @Override
            override def run() { h.publish() }
          }
        PCTHandle.timer.schedule(callback, delay.toLong)
      }
      case o => {
        val e = new Exception("Unknown event: " + o)
        e.printStackTrace()
      }
    }
  }

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
  /** A timer instance to implement Rwait events.
    */
  val timer: Timer = new Timer()
  // TODO: This is never shutdown.
}
