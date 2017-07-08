//
// Blockable.scala -- Scala trait Blockable
// Project OrcScala
//
// Created by amp on Dec 14, 2012.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core
import orc.Schedulable
import orc.error.OrcException
import orc.run.Logger

/** The interface that allows Schedulables to block on Blockers and receive a value when unblocked.
  *
  * Blockables can be unblocked by any thread. This means that as soon as
  * the Blocker knows about the Blockable and the Blocker is running you
  * must assume that it could unblock at any time. So in general, blocking
  * should be the last action before returning to the scheduler.
  *
  * @author amp
  */
trait Blockable extends Schedulable {
  /* The following actions are applied to Blockables to wake them.
   *
   *        P = Pruning, O = Otherwise, L = Closure, C = CallHandle
   *        These define what Blockers use what kind of awake.
   *
   * Publish a value                 PCL
   * Publish stop                    P
   * Halt                            OC
   * Unblock and simply continue     O
   * Unblock with an exception       C
   *
   * Tokens do all of these. Closures will only do publish of value and stop.
   */

  /** Called to wake up the blockable without providing any information. Just wake up to whatever you where doing.
    */
  def awake() {
    throw new AssertionError("Awake called on blockable that does not support awake without information (This is an interpreter bug).")
  }

  /** Called to wake up the blockable, but halt it immediately.
    *
    * This may be called after a called to awakeNonterminalValue, but during the same
    * invokation of check. Once this is called the blockable may reschedule
    * itself.
    *
    * This function should not block and should try to be fairly fast.
    */
  def halt() {
    throw new AssertionError("Halt called on non-haltable blockable (This is an interpreter bug).")
  }

  /** Called by the blocker from within its check method to notify the
    * Blockable that it has been unblocked and to provide the single
    * value that it might was waiting on. This must only be called while
    * executing on behalf of the Blockable.
    *
    * Semantically this is equivalent to "awakeNonterminalValue(v); halt();", however
    * for implementation reasons it is a separate primitive. Specifically,
    * there are cases in Token where awakeNonterminalValue would trigger an illegal
    * copy. This method must be used for all awakes from argument blockers:
    * Closure, and GraftGroup.
    */
  def awakeTerminalValue(v: AnyRef) {
    awakeNonterminalValue(v)
    halt()
  }

  /** Called by the blocker from within its check method to notify the
    * Blockable that it has been unblocked and to provide the value that
    * it might was waiting on. This must only be called while
    * executing on behalf of the Blockable.
    *
    * This does not terminate the blockable. It should go back to sleep
    * and expect to be awakened again by the same Blocker. In addition,
    * awakeNonterminalValue may be called more than once in response to a single call
    * to check. halt() will be called after all calls to awakeNonterminalValue
    * have completed.
    *
    * This function should not block and should try to be fairly fast.
    */
  def awakeNonterminalValue(v: AnyRef): Unit

  /** Called by the blocker from within its check method to notify the
    * Blockable that it has been unblocked and notify it that the value it
    * it was waiting on is Stop (this is similar to publish(None)).
    * This must only be called while executing on behalf of the Blockable.
    *
    * halt will not be called after awakeStop. The blocker will not call
    * the blockable again after this.
    *
    * This function should not block and should try to be fairly fast.
    */
  def awakeStop(): Unit

  /** Called by the blocker from within its check method to notify the
    * Blockable that it has been unblocked by an exception. This must only be called while
    * executing on behalf of the Blockable.
    *
    * The default implementation rethrows the exception.
    */
  def awakeException(e: OrcException) {
    throw e;
  }

  /** Called to tell the Blockable to block on the given blocker.
    * Called only while executing on behalf of the blockable.
    */
  def blockOn(b: Blocker): Unit

  /** Called to set this blockable to be quiescent. This should be reentrant
    * in the sense that calling this and unsetQuiescent in overlapping but matched
    * pairs should work as expected.
    */
  def setQuiescent(): Unit

  /** Called make the blockable non-quiescent.
    *
    * @see #setQuiescent()
    */
  def unsetQuiescent(): Unit
}
