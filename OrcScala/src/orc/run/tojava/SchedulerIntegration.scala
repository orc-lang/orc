package orc.run.tojava

import orc.Schedulable
import orc.run.Logger

/** A Schedulable which manages Context spawn/halt counting automatically.
  *
  * @author amp
  */
abstract class CounterSchedulable(c: Counter) extends Schedulable {
  // We known we are non-blocking because all Orc Java code is non-blocking.
  // TODO: Is this true? What about external site calls?
  override val nonblocking = true

  /** When we are scheduled prepare for spawning.
    */
  override def onSchedule() = {
    c.prepareSpawn() // Matched to: halt in onComplete
  }

  /** When execution completes halt.
    */
  override def onComplete() = {
    c.halt() // Matched to: prepareSpawn in onSchedule
  }
}

/** A subclass of ContextSchedulable that takes a run implementation as a Scala function.
  *
  * This is for Scala side use.
  *
  * @author amp
  */
final class CounterSchedulableFunc(c: Counter, f: () => Unit) extends CounterSchedulable(c) {
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
final class CounterSchedulableRunnable(c: Counter, f: Runnable) extends CounterSchedulable(c) {
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
