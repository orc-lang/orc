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
abstract class Join(inValues: Array[AnyRef]) {
  join =>
  /* This does not do real halts or spawns. Instead it assumes it can spawn
   * if needed and will always call halt or done (but not both) when it is
   * completed.
   */

  require(inValues.size > 0, "Join must have at least one argument. Check before call.")

  // The number of unbound values in values.
  protected var nUnbound = new AtomicInteger(inValues.size)
  // The array of values that have already been bound.
  // TODO: PERFORMANCE: Could this be the same array as inValues and we just overwrite values as we get them?
  // TODO: PERFORMANCE: This should have one blank spot at the beginning so that the array can be reused for the continuation call with the initial argument filled with the closure.
  //    The above two optimizations are NOT mutually exclusive. They can both be applied if the Force node knows to allocate the extra slot.
  //    These will be easiest to implement when I'm reworking Join to use specializable nodes for each element.
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
  }


  final def apply(): Array[AnyRef] = {
    // TODO: PERFORMANCE: It would probably perform slightly better to move this into an object method which only creates the actual join object if it is actually needed.
    //    The above would also enable reusing the incoming array in more cases.
    
    
    //Logger.finest(s"Starting join with: ${inValues.mkString(", ")}")
    
    // Start all the required forces.
    var nNonFutures = 0
    var halted = false
    for ((v, i) <- inValues.zipWithIndex) v match {
      case f: orc.Future => {
        f.get() match {
          case orc.FutureState.Bound(v) => {
            values(i) = v
            nNonFutures += 1
          }
          case orc.FutureState.Stopped => {
            // TODO: PERFORMANCE: Could break iteration here.
            halted = true
          }
          case orc.FutureState.Unbound => {
            Logger.finest(s"$join: Join joining on $f")
            // Force f so it will bind the correct index.
            val e = new JoinElement(i)
            f.read(e)
          }
        }
      }
      case _ => {
        // v is not a future so just bind it.
        values(i) = v
        nNonFutures += 1
      }
    }
    
    // Now decrement the unbound count by the number of non-futures we found.
    // And maybe finish immediately.
    if (halted) {
      if (join.halted.compareAndSet(false, true)) {
        return Join.HaltSentinel
      }
    } else if (nNonFutures > 0) {
      // Don't do this if it will not change the value. Otherwise this could
      // cause multiple calls to done.
      if (nUnbound.addAndGet(-nNonFutures) == 0 && join.halted.compareAndSet(false, true)) {
        return values
      }
    }
    return null
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


object Join {
  val HaltSentinel = Array[AnyRef]()
}