//
// Blocker.scala -- Scala trait Blocker
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core
import orc.OrcRuntime

/** The trait for objects that can be blocked on.
  *
  * Many subclasses of Blocker implement a read method (for example Closure and
  * PruningGroup). In addition to possibly blocking something on the Blocker,
  * read will also manipulate the Vclocks so that blocked tokens are not quiescent
  * while the Blocker is running. This is important because the Blocker may serve
  * as a bridge from one non-quiescent token to another, so non-quiescence must
  * be maintained.
  *
  * Blockers may unblock Blockables at any time in any thread. So as soon as you
  * block (for instance by calling PruningGroup.read or instantiating a SiteCallHandle)
  * on a Blocker you should assume you may be unblocked and running on another thread.
  *
  * @see Blockable
  *
  * @author dkitchin
  */
trait Blocker {
  /** Require that the blocker has a runtime reference because it will
    * always need one and making it consistent is nice.
    */
  val runtime: OrcRuntime

  /** When a Blockable blocked on this resource is scheduled,
    * it performs this check to observe any changes in
    * the state of this resource.
    *
    * This should call Blockable#awake(AnyRef) to notify the
    * Blockable.
    */
  def check(t: Blockable): Unit
}
