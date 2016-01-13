package orc.run.tojava

import java.util.{ Timer, TimerTask }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.function.{ BiConsumer, Consumer }
import java.util.logging.Level

import scala.util.parsing.input.Position

import orc.{ CaughtEvent, Handle, OrcEvent, OrcRuntime }
import orc.error.OrcException
import orc.lib.str.PrintEvent
import orc.run.Logger
import orc.run.extensions.RwaitEvent

// TODO: Somewhere there is a call to halt that does not match a call to prepareSpawn.
/** Context is the superclass of all execution contexts.
  *
  * The interface it provides allows spawning executions for later, halting,
  * and checking for termination. As such this subsumes groups, and the
  * runtime.
  *
  * @author amp
  */
abstract class Context extends Blockable {
  val runtime: OrcRuntime

  def publish(v: AnyRef): Unit
  def halt(): Unit
  def prepareSpawn(): Unit

  /** Spawn another execution.
    *
    * The odd choice of argument type is so that Java 8 code can call into it
    * easily.
    */
  def spawn(f: Consumer[Context]): Unit = {
    // Spawn is an expensive operation, so check for kill before we do it.
    checkLive();
    // Schedule the work. prepareSpawn and halt are called by
    // ContextSchedulableFunc.
    runtime.schedule(new ContextSchedulableFunc(this, () => f.accept(this)))
  }

  /** Spawn an execution and return future for it's first publication.
    *
    * This is very similar to spawn().
    */
  def spawnFuture(f: Consumer[Context]): Future = {
    checkLive();
    val fut = new Future(runtime)
    runtime.schedule(new ContextSchedulableFunc(this, () =>
      f.accept(new ContextBase(this) {
        // This special context just binds the future on publication.
        // Halting is passed through to this.
        override def publish(v: AnyRef): Unit = {
          fut.bind(v)
        }
      })))
    fut
  }

  /** Check that this context is live and throw KilledException if it is not.
    */
  def checkLive(): Unit = {
    if (!isLive()) {
      throw KilledException.SINGLETON
    }
  }

  /** Return true if this context is still live (has not been killed or halted
    * naturally).
    */
  def isLive(): Boolean
}

/** The base class of Context implementations with a parent context.
  *
  * This class provides method forwarding to the parent context.
  *
  * @author amp
  */
abstract class ContextBase(val parent: Context) extends Context {
  /** The runtime for this context.
    */
  val runtime: OrcRuntime = parent.runtime

  // TODO: The parent calls may be a performance problem because they have to go up the chain to the nearest implementation.

  def publish(v: AnyRef): Unit = {
    parent.publish(v)
  }

  def halt(): Unit = {
    parent.halt() // Matched to: calls to prepareSpawn by induction.
  }

  def prepareSpawn(): Unit = {
    parent.prepareSpawn() // Matched to: calls to halt by induction.
  }

  def isLive(): Boolean = {
    parent.isLive()
  }
}

/** An implementation of site call Handle which is also a tojava Context.
  */
final class ContextHandle(p: Context, val callSitePosition: Position) extends ContextBase(p) with Handle {
  /** Handle a site call publication.
    *
    * The semantics of a publication for a handle include halting so add that.
    */
  override def publish(v: AnyRef) = {
    // Catch and ignore killed exceptions since the site call itself should not be killed.
    try {
      super.publish(v)
    } catch {
      case _: KilledException => {}
    }
    halt() 
    // Matched to: Every invocation is required to be proceeded by a 
    //             prepareSpawn since it might spawn.
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
        ContextHandle.timer.schedule(callback, delay.toLong)
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
    halt()
    // Matched to: Every invocation is required to be proceeded by a 
    //             prepareSpawn since it might spawn.
  }

  // TODO: Support rights.
  def hasRight(rightName: String): Boolean = false
}

object ContextHandle {
  /** A timer instance to implement Rwait events.
    */
  val timer: Timer = new Timer()
  // TODO: This is never shutdown.
}

/** A context for the LHS of a branch combinator.
  *
  * The argument type is odd to allow simple calling from Java 8 code.
  * publishImpl effectively provides an implementation of the publish method.
  * This implementation is generally the RHS of the branch combinator.
  */
final class BranchContext(p: Context, publishImpl: BiConsumer[Context, AnyRef]) extends ContextBase(p) {

  override def publish(v: AnyRef): Unit = {
    publishImpl.accept(this, v)
  }
}

/** A mix-in to add spawn counting and halting detection support to a Context.
  *
  * This mix-in does not require a parent context.
  */
trait ContextCounterSupport extends Context {
  /** The number of executions that are either running or pending.
    *
    * This functions similarly to a reference count and this halts when count
    * reaches 0.
    */
  val count = new AtomicInteger(1)

  /** Decrement the count and check for overall halting.
    *
    * If we did halt call onContextHalted().
    */
  override def halt(): Unit = {
    val n = count.decrementAndGet()
    assert(n >= 0, "Halt is not allowed on already stopped CounterContexts")
    //Logger.finest(s"Decr $n in $this")
    if (n == 0) {
      onContextHalted();
    }
  }

  /** Increment the count.
    */
  override def prepareSpawn(): Unit = {
    val n = count.getAndIncrement()
    //Logger.finest(s"Incr $n in $this")
    assert(n > 0, "Spawning is not allowed once we go to zero count. No zombies allowed!!!")
  }

  /** Called when this whole context has halted.
    */
  def onContextHalted(): Unit
}

/** A base class for CounterContexts that do have a parent
  */
abstract class CounterContextBase(p: Context) extends ContextBase(p) with ContextCounterSupport {
  parent.prepareSpawn() // Matched to: halt in onContextHalted.

  def onContextHalted(): Unit = {
    // Notify parent that everything here has halted.
    parent.halt() // Matched to: prepareSpawn in constructor.
  }
}

/** A counter context that calls specific code when it halts fully.
  *
  * The argument type is odd to allow simple calling from Java 8 code.
  * ctxHaltImpl effectively provides an implementation of the onContextHalted
  * method however the superclass is always called after it to halt the parent
  * context. This implementation is generally the RHS of the concat combinator.
  */
final class CounterContext(p: Context, ctxHaltImpl: Consumer[Context]) extends CounterContextBase(p) {
  override def onContextHalted(): Unit = {
    ctxHaltImpl.accept(this)
    super.onContextHalted()
  }
}

/** The root of the context tree. Analogous to Execution.
  *
  * It implements top-level publication and halting.
  */
final class RootContext(val runtime: OrcRuntime) extends Context with ContextCounterSupport {
  /** Simply print top-level publications.
    */
  override def publish(v: AnyRef): Unit = {
    // TODO: this should pass it into the runtime to allow top-level publication to be handled more nicely.
    println(v)
  }

  /** When we halt stop the scheduler and notify anyone who cares.
    */
  def onContextHalted(): Unit = {
    Logger.info("Top level context complete.")
    runtime.stopScheduler()
    synchronized { notifyAll() }
  }

  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized { while (isLive()) wait() }
  }

  /** Define liveness as when this is totally halted.
    *
    * This will never cause it's subcontexts to halt.
    */
  def isLive() = count.get > 0
}

/** The terminator context for implementing trim.
  *
  * This contains a flag which is set by the first publication. Once this flag
  * is set all publications are ignored and isLive returns false.
  */
final class TerminatorContext(p: Context) extends ContextBase(p) {
  val isLiveFlag = new AtomicBoolean(true)

  override def isLive() = {
    // If we are alive and our parent is.
    // TODO: This super call means you always have to traverse to the top to check liveness. This is probably too expensive.
    isLiveFlag.get && super.isLive()
  }

  override def publish(v: AnyRef) {
    if (isLiveFlag.compareAndSet(true, false)) {
      // If the flag was equal to true, then publish.
      super.publish(v)
      // Do not throw the exception here since we don't actually need it (since we just halted) and exceptions are slow.
    } else {
      // Throw the terminated exception.
      checkLive()
    }
  }
}


