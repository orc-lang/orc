//
// ClockCallHandle.scala -- Scala class/trait/object ClockCallHandle
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 26, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

/**
 * 
 * A call handle specific to the Await operation on a virtual clock.
 *
 * @author dkitchin
 */
class ClockCallHandle(caller: Token) extends CallHandle(caller) {

  /* 
   * A token only blocks on this handle if the Await call was for a future time.
   * Thus, whenever a token is blocked on this handle, it is also quiescent. 
   */
  val quiescentWhileBlocked = true
  
  def run() { /* Running a clock handle does nothing. */ }
  
}