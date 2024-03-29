//
// Blocker.scala -- Scala trait Blocker
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

/** The trait for objects that can be blocked on.
  *
  * Many subclasses of Blocker implement a read method (for example Closure and
  * GraftGroup). In addition to possibly blocking something on the Blocker,
  * read will also manipulate the Vclocks so that blocked tokens are not quiescent
  * while the Blocker is running. This is important because the Blocker may serve
  * as a bridge from one non-quiescent token to another, so non-quiescence must
  * be maintained.
  *
  * Blockers may unblock Blockables at any time in any thread. So as soon as you
  * block (for instance by calling Future.read or instantiating a SiteCallController)
  * on a Blocker you should assume you may be unblocked and running on another thread.
  *
  * @see Blockable
  *
  * @author dkitchin
  */
trait Blocker {
  /** When a Blockable blocked on this resource is scheduled,
    * it performs this check to observe any changes in
    * the state of this resource. Blockables that call this, should
    * assume they may have been rescheduled when this returns, however
    * during the execution of check the Blockable may assume it has
    * not yet been rescheduled.
    *
    * This should call Blockable#awake(AnyRef) to notify the
    * Blockable.
    */
  def check(t: Blockable): Unit
}

trait ReadableBlocker extends Blocker {
  /** Block t on this if it is not yet bound otherwise immediately awake t.
    */
  def read(t: Blockable): Unit
}
