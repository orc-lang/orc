package orc.run.tojava

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

/** Join a number of futures by blocking on all of them simultaneously.
  *
  * This is analogous to orc.run.core.Join.
  *
  * @param inValues a list of value which may contain futures. All the futures in this array will be blocked on.
  *
  * This class must be subclassed to implement halt and done.
  *
  * @author amp
  */
abstract class Join(inValues: Array[AnyRef]) {
  join =>
  /* This does not do real halts or spawns. Instead it assumes it can spawn
   * if needed and will always call halt or done (but not both) when it is
   * completed.
   */

  // The number of unbound values in values.
  var nUnbound = new AtomicInteger(inValues.size)
  // The array of values that have already been bound.
  val values = Array.ofDim[AnyRef](inValues.size)
  // The flag saying if we have already halted.
  var halted = new AtomicBoolean(false)

  /** A Blockable that binds a specific element of values in publish().
    */
  final class JoinElement(i: Int) extends Blockable {
    /** Bind value i to v.
      *
      * If join has not halted we bind and check if we are done.
      */
    def publish(v: AnyRef): Unit = {
      // Check if we are halted then bind. This is an optimization since 
      // nUnbound can never reach 0 if we halted.
      if (!halted.get()) {
        // Bind the value (this is not synchronized because checkComplete 
        // will cause the read of it, enforcing the needed ordering).
        values(i) = v
        // TODO: Does this write need to be volatile?
        // Not decrement the number of unbound values and see if we are done.
        join.checkComplete(nUnbound.decrementAndGet())
      }
    }
    /** Halt the whole join if it has not yet been halted.
      */
    def halt(): Unit = {
      // Halt if we have not already halted.
      if (halted.compareAndSet(false, true)) {
        join.halt()
      }
    }

    // We can ignore prepareSpawn since all the executions are being performed
    // in the context of the count held by join.
    def prepareSpawn(): Unit = {}
  }

  // Start all the required forces.
  var nNonFutures = 0
  for ((v, i) <- inValues.zipWithIndex) v match {
    case f: Future => {
      // Force f so it will bind the correct index.
      f.forceIn(new JoinElement(i))
    }
    case _ => {
      // v is not a future so just bind it.
      values(i) = v
      nNonFutures += 1
    }
  }
  // Now decrement the unbound count by the number of non-futures we found.
  // And maybe finish immediately.
  checkComplete(nUnbound.addAndGet(-nNonFutures))

  /** Check if we are done by looking at n which must be the current number of
    * unbound values.
    */
  final def checkComplete(n: Int): Unit = {
    assert(n >= 0)
    if (n == 0) {
      done()
    }
  }

  /** Handle a successful completion.
    *
    * This should use values to get the bound values.
    */
  def done(): Unit

  /** Handle a halting case.
    *
    * values will not be completely bound.
    */
  def halt(): Unit
}

