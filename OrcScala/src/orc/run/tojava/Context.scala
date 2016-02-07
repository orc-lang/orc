package orc.run.tojava

import java.util.{ Timer, TimerTask }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.function.{ BiConsumer, Consumer }
import java.util.logging.Level
import scala.util.parsing.input.Position
import orc.OrcRuntime
import orc.run.Logger
import orc.run.core.EventHandler
import orc.values.OrcRecord
import orc.values.Field
import orc.Handle
import orc.OrcEvent
import orc.PublishedEvent

/** The root of the context tree. Analogous to Execution.
  *
  * It implements top-level publication and halting.
  */
final class Execution(runtime: ToJavaRuntime, protected var eventHandler: OrcEvent => Unit) extends EventHandler {
  root =>
  private var isDone = false
  
  runtime.addExecution(root)
  runtime.installHandlers(this)

  val p: Continuation = new Continuation {
    def call(v: AnyRef): Unit = {
      notifyOrc(PublishedEvent(v))
    }
  }

  val c: Counter = new Counter {
    /** When we halt stop the scheduler and notify anyone who cares.
      */
    def onContextHalted(): Unit = {
      Logger.fine("Top level context complete.")
      runtime.removeExecution(root)
      root.synchronized {
        isDone = true
        root.notifyAll()
      }
    }
  }

  val t = new Terminator

  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized {
      while (!isDone) wait()
    }
  }

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
    val fut = new Future()
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
          case Some(w) => p.call(w)
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

  final def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) = runtime.invoke(h, v, vs)
}


