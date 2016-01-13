package orc.run.tojava

import orc.Schedulable
import orc.run.Logger

/** A Schedulable which manages Context spawn/halt counting automatically.
  *
  * @author amp
  */
abstract class ContextSchedulable(ctx: Context) extends Schedulable {
  // We known we are non-blocking because all Orc Java code is non-blocking.
  override val nonblocking = true

  /** When we are scheduled prepare for spawning.
    */
  override def onSchedule() = {
    ctx.prepareSpawn() // Matched to: halt in onComplete
  }

  /** When execution completes halt.
    */
  override def onComplete() = {
    ctx.halt() // Matched to: prepareSpawn in onSchedule
  }
}

/** A subclass of ContextSchedulable that takes a run implementation as a Scala function.
  *
  * This is for Scala side use.
  *
  * @author amp
  */
final class ContextSchedulableFunc(ctx: Context, f: () => Unit) extends ContextSchedulable(ctx) {
  /** Call the provided implementation and ignore KilledException.
    *
    * The exception is ignored because if something is killed it is fine to
    * just die all the way to the scheduler, but it should not effect the
    * scheduler thread.
    */
  def run(): Unit = {
    // Catch kills and continue.
    try {
      f()
    } catch {
      case _: KilledException =>
        ()
    }
  }
}

/** A subclass of ContextSchedulable that takes a run implementation as a Java Runnable.
  *
  * This is for Java 8 side use.
  *
  * @author amp
  */
final class ContextSchedulableRunnable(ctx: Context, f: Runnable) extends ContextSchedulable(ctx) {
  /** Call the provided implementation and ignore KilledException.
    *
    * The exception is ignored because if something is killed it is fine to
    * just die all the way to the scheduler, but it should not effect the
    * scheduler thread.
    */
  def run(): Unit = {
    // Catch kills and continue.
    try {
      f.run()
    } catch {
      case _: KilledException =>
        ()
    }
  }
}
