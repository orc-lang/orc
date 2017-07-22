package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import orc.FutureReader
import orc.run.porce.Logger

// TODO: Try to remove redundency between this and Join.

/** Join a number of futures by blocking on all of them simultaneously waiting for them to ALL either be
  * bound or halt.
  *
  * @param inValues a list of value which may contain futures. All the futures in this array will be blocked on.
  *
  * This class must be subclassed to implement done.
  *
  * @author amp
  */
abstract class Resolve(inValues: Array[AnyRef]) {
  resolve =>
  /* This does not do real halts or spawns. Instead it assumes it can spawn
   * if needed and will always call halt or done (but not both) when it is
   * completed.
   */

  require(inValues.size > 0, "Resolve must have at least one argument. Check before call.")

  // The number of unbound values in values.
  var nUnbound = new AtomicInteger(inValues.size)

  /** A Blockable that binds a specific element of values in publish().
    */
  final class JoinElement() extends FutureReader {
    /** The flag used to make sure publish/halt is only called once.
      */
    var bound = new AtomicBoolean(false)

    def publish(v: AnyRef): Unit = halt()

    def halt(): Unit = if (bound.compareAndSet(false, true)) {
      Logger.finest(s"$resolve: Resolve joined")
      checkComplete(nUnbound.decrementAndGet())
    }

    // We can ignore prepareSpawn since all the executions are being performed
    // in the context of the count held by join.
    def prepareSpawn(): Unit = {}
  }

  final def apply() = {
    // Start all the required forces.
    var nNonFutures = 0
    for (v <- inValues) v match {
      case f: Future => {
        Logger.finest(s"$resolve: Resolve joining on $f")
        val e = new JoinElement()
        f.read(e)
      }
      case _ => {
        nNonFutures += 1
      }
    }
  
    // Now decrement the unbound count by the number of non-futures we found.
    // And maybe finish immediately.
    if (nNonFutures > 0)
      // Don't do this if it will not change the value. Otherwise this could
      // cause multiple calls to done.
      checkComplete(nUnbound.addAndGet(-nNonFutures))
  
    Logger.finest(s"$resolve: Resolve setup with (${inValues.mkString(", ")}) and nonfut=$nNonFutures and unbound=$nUnbound")
  }

  /** Check if we are done by looking at n which must be the current number of
    * unbound values.
    */
  final def checkComplete(n: Int): Unit = {
    assert(n >= 0, n)
    if (n == 0) {
      Logger.finest(s"$resolve: Resolve finished")
      done()
    }
  }

  /** Handle a successful completion.
    */
  def done(): Unit
}
