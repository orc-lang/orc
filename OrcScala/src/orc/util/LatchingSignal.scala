//
// LatchingSignal.scala -- Scala class LatchingSignal
// Project OrcScala
//
// Created by jthywiss on Aug 7, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

/** A "proceed" signal for use across Threads.
  * Once signaled, all waiting calls to await will return,
  * and all future calls to await will return immediately.
  *
  * @author jthywiss
  */
class LatchingSignal() {
  var signalled = false

  /** Wait until signal has been invoked.  If signal
    * has been invoked, then return immediately.
    */
  def await() = synchronized {
    while (!signalled) wait()
  }

  /** Notify all waiting and future callers of await to proceed.
    * This method is is idempotent.
    */
  def signal() = synchronized {
    signalled = true
    notifyAll()
  }

}
