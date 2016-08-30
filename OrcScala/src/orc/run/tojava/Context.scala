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
import orc.values.sites.HasFields
import orc.error.runtime.NoSuchMemberException
import orc.run.tojava.Wrapper
import orc.CaughtEvent
import orc.values.sites.JavaCall
import orc.values.OrcValue

/** The root of the context tree. Analogous to Execution.
  *
  * It implements top-level publication and halting.
  */
final class Execution(val runtime: ToJavaRuntime, protected var eventHandler: OrcEvent => Unit) extends EventHandler {
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
  def spawnFuture(c: Counter, t: Terminator, f: BiConsumer[Continuation, Counter]): Future = {
    t.checkLive();
    val fut = new Future()
    runtime.schedule(new CounterSchedulableFunc(c, () => {
      val p = new Continuation {
        // This special context just binds the future on publication.
        override def call(v: AnyRef): Unit = {
          fut.bind(v)
        }
      }
      val newC = new CounterNestedBase(c) {
        // Initial count matches: halt() in finally below.
        override def onContextHalted(): Unit = {
          fut.stop()
          super.onContextHalted()
        }
      }
      try {
        f.accept(p, newC)
      } finally {
        // Matches: the starting count of newC
        newC.halt()
      }
    }))
    fut
  }

  private final class PCJoin(p: Continuation, c: Counter, vs: Array[AnyRef], forceClosures: Boolean) extends Join(vs, forceClosures) {
    Logger.finer(s"Spawn for PCJoin $this (${vs.mkString(", ")})")
    
    def done(): Unit = {
      Logger.finer(s"Done for PCJoin $this (${values.mkString(", ")})")
      // Prevent KilledException from propagating into the code using the Blockable.
      try {
        p.call(values)
      } catch {
        case _: KilledException => {}
      } finally {
        // Matches: Call to prepareSpawn in constructor
        c.halt()
      }
    }
    def halt(): Unit = {
      Logger.finer(s"Halt for PCJoin $this (${values.mkString(", ")})")
      c.halt()
    }
  }
  
  /** Force a list of values: forcing futures and resolving closures.
    * 
    * If vs contains a ForcableCallableBase it must have a correct and complete closedValues.
    */
  def force(p: Continuation, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    // Matches: call to halt in done and halt in PCJoin
    // This is here because done and halt can be called from the superclass initializer. Damn initialation order.
    c.prepareSpawn()
    new PCJoin(p, c, vs, true)
  }

  /** Force a list of values: forcing futures and ignoring closures.
    */
  def forceForCall(p: Continuation, c: Counter, t: Terminator, vs: Array[AnyRef]): Unit = {
    // Matches: call to halt in done and halt in PCJoin
    // This is here because done and halt can be called from the superclass initializer. Damn initialation order.
    c.prepareSpawn()
    new PCJoin(p, c, vs, false)
  }

  /** Get a field of an object if possible.
    *
    * This may result in a site call and hence a spawn. However all the halts
    * and stuff are handled internally. However it does mean that this
    * returning does not imply this has halted.
    */
  def getField(p: Continuation, c: Counter, t: Terminator, v: AnyRef, f: Field) = {
    Wrapper.unwrap(v) match {
      case r: HasFields if r.hasField(f) => {
        p.call(r.getField(f))
      }
      case _: OrcValue => {
        // OrcValues must HasFields to have fields accessed.
        notifyOrc(CaughtEvent(new NoSuchMemberException(v, f.field)))
      }
      case o => {
        // TODO: This should actually check if it exists and throw the error here:
        // notifyOrc(CaughtEvent(new NoSuchMemberException(v, f.field)))
        p.call(JavaCall.getField(o, f))
      }
    }
  }

  final def invoke(h: Handle, v: AnyRef, vs: List[AnyRef]) = {
    assert(if (vs.size == 1) !vs.head.isInstanceOf[Field] else true) 
    runtime.invoke(h, v, vs)
  }
}


