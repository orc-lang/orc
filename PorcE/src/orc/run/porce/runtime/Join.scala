//
// Join.scala -- Scala classes Join and Resolve
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import orc.FutureReader
import orc.FutureBound
import orc.FutureStopped
import orc.FutureUnbound
import orc.run.porce.Logger

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
abstract class Join(inValues: Array[AnyRef], forceClosures: Boolean) {
  join =>
  /* This does not do real halts or spawns. Instead it assumes it can spawn
   * if needed and will always call halt or done (but not both) when it is
   * completed.
   */

  require(inValues.size > 0, "Join must have at least one argument. Check before call.")

  // The number of unbound values in values.
  protected var nUnbound = new AtomicInteger(inValues.size)
  // The array of values that have already been bound.
  val values = Array.ofDim[AnyRef](inValues.size)
  // The flag saying if we have already halted.
  protected val halted = new AtomicBoolean(false)

  /** A Blockable that binds a specific element of values in publish().
    */
  final private class JoinElement(i: Int) extends FutureReader {
    /** The flag used to make sure publish/halt is only called once.
      */
    var bound = new AtomicBoolean(false)

    /** Bind value i to v if this is not yet bound.
      *
      * If join has not halted we bind and check if we are done.
      */
    def publish(v: AnyRef): Unit = if (bound.compareAndSet(false, true)) {
      Logger.finest(s"$join: Join joined to $v ($i)")
      // Check if we are halted then bind. This is an optimization since
      // nUnbound can never reach 0 if we halted.
      if (!join.halted.get()) {
        // Bind the value (this is not synchronized because checkComplete
        // will cause the read of it, enforcing the needed ordering).
        values(i) = v
        // TODO: Does this write need to be volatile?
        // Now decrement the number of unbound values and see if we are done.
        join.checkComplete(join.nUnbound.decrementAndGet())
      }
    }

    /** Halt the whole join if it has not yet been halted and this has not yet
      * been bound.
      */
    def halt(): Unit = if (bound.compareAndSet(false, true)) {
      Logger.finest(s"$join: Join halted ($i)")
      // Halt if we have not already halted.
      if (join.halted.compareAndSet(false, true)) {
        //Logger.finest(s"Finished join with halt")
        join.halt()
      }
    }

    // We can ignore prepareSpawn since all the executions are being performed
    // in the context of the count held by join.
    def prepareSpawn(): Unit = {}
  }

  //Logger.finest(s"Starting join with: ${inValues.mkString(", ")}")

  final def apply() = {
    // Start all the required forces.
    var nNonFutures = 0
    for ((v, i) <- inValues.zipWithIndex) v match {
      case f: orc.Future => {
        Logger.finest(s"$join: Join joining on $f")
        // Force f so it will bind the correct index.
        val e = new JoinElement(i)
        // TODO: PERFORMANCE: It's possible this could be improved by checking if f is resolved and using special handling. But it may not matter.
        f.read(e)
      }
      case _ => {
        // v is not a future so just bind it.
        values(i) = v
        nNonFutures += 1
      }
    }
    
    // Now decrement the unbound count by the number of non-futures we found.
    // And maybe finish immediately.
    if (nNonFutures > 0)
      // Don't do this if it will not change the value. Otherwise this could
      // cause multiple calls to done.
      checkComplete(nUnbound.addAndGet(-nNonFutures))
  }

  /** Check if we are done by looking at n which must be the current number of
    * unbound values.
    */
  private final def checkComplete(n: Int): Unit = {
    assert(n >= 0, n)
    if (n == 0 && halted.compareAndSet(false, true)) {
      Logger.finest(s"$join: Join finished with: ${values.mkString(", ")}")
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
