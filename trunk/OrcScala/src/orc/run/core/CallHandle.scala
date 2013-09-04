//
// CallHandle.scala -- Scala class CallHandle
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 26, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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
  *
  * All descendants of CallHandle must maintain a scheduling invariant:
  * it must not be possible for the handle to reschedule the calling token
  * until the calling thread enters the onComplete method of the token.
  * 
  * SiteCallHandle maintains this invariant by staging itself on the caller,
  * so that it cannot be scheduled to run until after the caller has completed.
  * 
  * AwaitCallHandle maintains this invariant because the calling token keeps
  * its governing clock from becoming quiescent until the token itself becomes 
  * quiescent in its onComplete method.
  *
  * @author dkitchin
  */
abstract class CallHandle(val caller: Token) extends Handle with Blocker {
   // This is the only Blocker that can produce exceptions.

  protected var state: CallState = CallInProgress
  
  val runtime = caller.runtime

  /* Returns true if the state transition was made, 
   * false otherwise (e.g. if the handle was already in a final state)
   */
  protected def setState(newState: CallState): Boolean = {
    synchronized {
      if (isLive) {
        state = newState
        runtime.schedule(caller)
        true
      } else {
        false
      }
    }
  }

  def publish(v: AnyRef) { setState(CallReturnedValue(v)) }
  def halt() { setState(CallSilent) }
  def !!(e: OrcException) { setState(CallRaisedException(e)) }

  def callSitePosition = caller.sourcePosition
  def hasRight(rightName: String) = caller.options.hasRight(rightName)

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
      setState(CallWasKilled)
    }
  }

  def check(t: Blockable) {
    val callState = synchronized { state }
    callState match {
      case CallInProgress => { throw new AssertionError("Spurious check of call handle. "+this+".state=" + this.state) }
      case CallReturnedValue(v) => { t.awakeValue(v) } // t.publish(v) sort of
      case CallSilent => { t.halt() } // t.halt()
      case CallRaisedException(e) => { t.awakeException(e) } // t !! e
      case CallWasKilled => {}
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
