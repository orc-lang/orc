//
// Blockable.scala -- Scala trait Blockable
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava

/** Blockable represents any object that can block on something and then
  * handle the result.
  *
  * @author amp
  */
trait Blockable {
  /** Unblock with a publication.
    *
    * This does not imply halting: instead halt will be called as well if an
    * explicit halt is needed.
    */
  def publish(v: AnyRef): Unit

  /** Halt the Blockable.
    *
    * This can occur without or without a publication. Every call to this must
    * match a call to prepareSpawn().
    */
  def halt(): Unit

  /** Setup for a later execution possibly in another thread.
    *
    * This doesn't actually spawn anything, but prepares for waiting for
    * execution in another thread of control, such as spawning a new execution.
    *
    * Every call to this must be matched by a later call to halt().
    */
  def prepareSpawn(): Unit
}
