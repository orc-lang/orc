package orc.run.tojava

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import orc.run.Logger

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
    /** The flag used to make sure publish/halt is only called once.
      */
    var bound = new AtomicBoolean(false)

    /** Bind value i to v if this is not yet bound.
      *
      * If join has not halted we bind and check if we are done.
      */
    def publish(v: AnyRef): Unit = if (bound.compareAndSet(false, true)) {
      //Logger.finest(s"JoinElement $i published: $v")
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

    /** Halt the whole join if it has not yet been halted and this has not yet
      * been bound.
      */
    def halt(): Unit = if (bound.compareAndSet(false, true)) {
      //Logger.finest(s"JoinElement $i halted")
      // Halt if we have not already halted.
      if (halted.compareAndSet(false, true)) {
        //Logger.finest(s"Finished join with halt")
        join.halt()
      }
    }

    // We can ignore prepareSpawn since all the executions are being performed
    // in the context of the count held by join.
    def prepareSpawn(): Unit = {}
  }
  
  //Logger.finest(s"Starting join with: ${inValues.mkString(", ")}")

  // Start all the required forces.
  var nNonFutures = 0
  for ((v, i) <- inValues.zipWithIndex) v match {
    case f: Future => {
      // Force f so it will bind the correct index.
      // TODO: This is a hack. Not sure how to fix it though. The adding of the element and the check for completeness of the future must be atomic.
      val e = new JoinElement(i)
      val filled = f.forceIn(e)
      if (filled)
        e.halt()
    }
    case _ => {
      // v is not a future so just bind it.
      values(i) = v
      nNonFutures += 1
    }
  }
  // Now decrement the unbound count by the number of non-futures we found.
  // And maybe finish immediately.
  if(nNonFutures > 0)
    // Don't do this if it will not change the value. Otherwise this could
    // cause multiple calls to done.
    checkComplete(nUnbound.addAndGet(-nNonFutures))

  /** Check if we are done by looking at n which must be the current number of
    * unbound values.
    */
  final def checkComplete(n: Int): Unit = {
    assert(n >= 0)
    if (n == 0) {
      //Logger.finest(s"Finished join with: ${values.mkString(", ")}")
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
  /* This does not do real halts or spawns. Instead it assumes it can spawn
   * if needed and will always call halt or done (but not both) when it is
   * completed.
   */

  // The number of unbound values in values.
  var nUnbound = new AtomicInteger(inValues.size)

  /** A Blockable that binds a specific element of values in publish().
    */
  final class JoinElement() extends Blockable {
    /** The flag used to make sure publish/halt is only called once.
      */
    var bound = new AtomicBoolean(false)

    def publish(v: AnyRef): Unit = halt()

    def halt(): Unit = if (bound.compareAndSet(false, true)) {
      checkComplete(nUnbound.decrementAndGet())
    }

    // We can ignore prepareSpawn since all the executions are being performed
    // in the context of the count held by join.
    def prepareSpawn(): Unit = {}
  }

  // Start all the required forces.
  var nNonFutures = 0
  for (v <- inValues) v match {
    case f: Future => {
      f.forceIn(new JoinElement())
    }
    case _ => {
      nNonFutures += 1
    }
  }
  // Now decrement the unbound count by the number of non-futures we found.
  // And maybe finish immediately.
  if(nNonFutures > 0)
    // Don't do this if it will not change the value. Otherwise this could
    // cause multiple calls to done.
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
    */
  def done(): Unit
}

