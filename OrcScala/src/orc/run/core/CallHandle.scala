//
// CallHandle.scala -- Scala class CallHandle
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

import orc.error.OrcException
import orc.Handle

/** An abstract call handle for any call made by a token.
  *
  * @author dkitchin
  */
abstract class CallHandle(val caller: Token) extends Handle with Blocker {

  protected var state: CallState = CallInProgress

  /* Returns true if the state transition was made, 
   * false otherwise (e.g. if the handle was already in a final state)
   */
  protected def setState(newState: CallState): Boolean = {
    synchronized {
      if (isLive) {
        state = newState
        caller.schedule()
        true
      } else {
        false
      }
    }
  }

  def publish(v: AnyRef) { setState(CallReturnedValue(v)) }
  def halt() { setState(CallSilent) }
  def !!(e: OrcException) { setState(CallRaisedException(e)) }

  def notifyOrc(event: orc.OrcEvent) {
    synchronized {
      if (isLive) {
        caller.notifyOrc(event)
      }
    }
  }

  def isLive = synchronized { !state.isFinal }

  def kill() {
    synchronized {
      state = CallWasKilled
    }
  }

  def check(t: Token) {
    val callState = synchronized { state } 
    callState match {
      case CallInProgress => { t.scheduledBy.printStackTrace(); throw new AssertionError("Spurious check of call handle. state="+this.state) }
      case CallReturnedValue(v) => { t.publish(v) }
      case CallSilent => { t.halt() }
      case CallRaisedException(e) => { t !! e }
      case CallWasKilled => { }
    }
  }

}

/** Possible states of a CallHandle */
trait CallState { val isFinal: Boolean }

trait NonterminalCallState extends CallState { val isFinal = false }
case object CallInProgress extends NonterminalCallState

trait TerminalCallState extends CallState { val isFinal = true }
case class CallReturnedValue(v: AnyRef) extends TerminalCallState
case class CallRaisedException(e: OrcException) extends TerminalCallState
case object CallSilent extends TerminalCallState
case object CallWasKilled extends TerminalCallState
