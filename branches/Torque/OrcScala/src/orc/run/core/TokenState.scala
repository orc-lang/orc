//
// TokenState.scala -- Scala class/trait/object TokenState
// Project OrcScala
//
// $Id$
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

/**
 * 
 *
 * @author dkitchin
 */
trait TokenState {
  val isQuiescent: Boolean
  val isLive: Boolean
}

/** Token is ready to make progress */
case object Live extends TokenState { 
  val isQuiescent = false 
  val isLive = true
}

/** Token is propagating a published value */
case class Published(v: AnyRef) extends TokenState {
  val isQuiescent = false
  val isLive = true
}

/** Token is waiting on another task */
case class Blocked(blocker: Blocker) extends TokenState { 
  val isQuiescent = blocker.quiescentWhileBlocked
  val isLive = true
}

/** Token has been told to suspend, but it's still in the scheduler queue */
case class Suspending(prevState: TokenState) extends TokenState {
  val isQuiescent = prevState.isQuiescent
  val isLive = prevState.isLive
}

/** Suspended Tokens must be re-scheduled upon resume */
case class Suspended(prevState: TokenState) extends TokenState {
  val isQuiescent = prevState.isQuiescent
  val isLive = prevState.isLive
}

/** Token halted itself */
case object Halted extends TokenState {
  val isQuiescent = true
  val isLive = false
}

/** Token killed by engine */
case object Killed extends TokenState {
  val isQuiescent = true
  val isLive = false
}