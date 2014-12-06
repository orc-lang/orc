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

  protected var state: CallState = CallInProgress(List())
  protected var quiescent = false

  val runtime = caller.runtime

  /* Returns true if the state transition was made,
   * false otherwise (e.g. if the handle was already in a final state)
   * 
   * Should always be called with the monitor on this held.
   */
  protected def setState(newState: CallState): Boolean = {
    if (newState.isFinal && quiescent) {
      quiescent = false
      caller.unsetQuiescent()
    }
    if (isLive) {
      // If the state already has publications then the caller will already be scheduled.
      // So schedule it if we are either adding publications to an empty state or finalizing an empty state.
      if (state.publications.isEmpty && (newState.publications.nonEmpty || newState.isFinal))
        runtime.schedule(caller)
      state = newState
      true
    } else {
      false
    }
  }

  def publishNonterminal(v: AnyRef) = synchronized {
    setState(CallInProgress(v +: state.publications))
  }
  def halt() = synchronized {
    assert(state.isInstanceOf[CallInProgress] || state == CallWasKilled, state.toString)
    setState(CallHalted(state.publications))
  }
  def !!(e: OrcException) = synchronized {
    setState(CallRaisedException(e))
  }

  def callSitePosition = caller.sourcePosition
  def hasRight(rightName: String) = caller.options.hasRight(rightName)

  def notifyOrc(event: orc.OrcEvent) = synchronized {
    if (isLive) {
      caller.notifyOrc(event)
    }
  }

  def setQuiescent() = synchronized {
    state match {
      case CallInProgress(_) if !quiescent => {
        quiescent = true
        caller.setQuiescent()
      }
      case CallWasKilled => {}
      case _ => throw new AssertionError(s"Handle.setQuiescent in bad state: state=$state, quiescent=$quiescent")
    }
  }

  def isLive = synchronized { !state.isFinal }

  def kill(): Unit = synchronized {
    setState(CallWasKilled)
  }

  /* Note from Arthur: There is a small chance this function being fully synchronized will introduce a 
   * deadlock. However I think the old deadlock was caused by the old lazy recursive closure resolution, 
   * which has now been replaced. However if there are any places where mutually referential objects
   * call read or similar in their respective awake* methods then this could cause a dead lock.
   */
  def check(t: Blockable) = synchronized {
    for (v <- state.publications.reverseIterator) {
      t.awakeNonterminalValue(v)
    }

    state match {
      case CallInProgress(Nil) => { throw new AssertionError("Spurious check of call handle. " + this + ".state=" + this.state) }
      case CallInProgress(_) => {} // Already handled fully by parts above and below
      case CallHalted(_) => { t.halt() }
      case CallRaisedException(e) => { t.awakeException(e) } // t !! e
      case CallWasKilled => {}
    }
    
    state = state.withoutPublications
  }
  
  override def toString = synchronized {
    super.toString + s"($state)"
  }

}

/** Possible states of a CallHandle */
trait CallState {
  val isFinal: Boolean
  // The publications that have not been read in reverse order
  val publications: Seq[AnyRef] = Nil

  def withoutPublications: CallState = this
}

trait NonterminalCallState extends CallState { val isFinal = false }
case class CallInProgress(_publications: Seq[AnyRef]) extends NonterminalCallState {
  override val publications = _publications
  override def withoutPublications = copy(Nil)
}

trait TerminalCallState extends CallState { val isFinal = true }
case class CallRaisedException(e: OrcException) extends TerminalCallState
case class CallHalted(_publications: Seq[AnyRef]) extends TerminalCallState {
  override val publications = _publications
  override def withoutPublications = copy(Nil)
}
case object CallWasKilled extends TerminalCallState
