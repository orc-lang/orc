package orc.run.tojava

import java.util.{ Timer, TimerTask }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.function.{ BiConsumer, Consumer }
import java.util.logging.Level
import scala.util.parsing.input.Position
import orc.OrcRuntime
import orc.run.Logger

/** The root of the context tree. Analogous to Execution.
  *
  * It implements top-level publication and halting.
  */
final class RootContext(runtime: ToJavaRuntime) {
  private var isDone = false

  val p: Continuation = new Continuation {
    def call(v: AnyRef): Unit = {
      // TODO: this should pass it into the runtime to allow top-level publication to be handled more nicely.
      println(v)
    }
  }

  val c: Counter = new Counter {
    /** When we halt stop the scheduler and notify anyone who cares.
      */
    def onContextHalted(): Unit = {
      Logger.info("Top level context complete.")
      runtime.runtime.stopScheduler()
      synchronized {
        isDone = true
        notifyAll()
      }
    }
  }

  val t = new Terminator

  /** Block until this context halts.
    */
  def waitForHalt(): Unit = {
    synchronized { while (!isDone) wait() }
  }
}


