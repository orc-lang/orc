//
// CallHandle.scala -- Scala class CallHandle
// Project OrcScala
//
// Created by dkitchin on Aug 26, 2011.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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
  val execution = caller.execution

  // a mechanism to delay schedule of caller until later without holding the lock.
  var scheduleHeld = false
  var scheduleOnRelease = false

  /** Set the CallHandle to hold scheduling the caller until releaseSchedule is called.
    *
    * Call inside a synchronized block only.
    */
  private def holdSchedule() = {
    assert(!scheduleHeld)
    assert(!scheduleOnRelease)
    scheduleHeld = true
  }

  /** Schedule the caller either now or later depending on the hold state.
    *
    * Call inside a synchronized block only.
    */
  private def schedule() = {
    if (scheduleHeld)
      scheduleOnRelease = true
    else
      runtime.schedule(caller)
  }

  /** Allow scheduling the caller again and possibly schedule it.
    *
    * Call inside a synchronized block only.
    */
  private def releaseSchedule() = {
    assert(scheduleHeld)
    if (scheduleOnRelease)
      runtime.schedule(caller)
    scheduleHeld = false
    scheduleOnRelease = false
  }

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
        schedule()
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
    // A second call to this can occur because we are trying to halt a handle for an OrcSite call. 
    // And it's idempotent, so it shouldn't matter.
    //assert(state.isInstanceOf[CallInProgress] || state == CallWasKilled, state.toString)
    setState(CallHalted(state.publications))
  }
  def discorporate() = synchronized {
    assert(state.isInstanceOf[CallInProgress] || state == CallWasKilled, state.toString)
    caller.discorporate()
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

  def check(t: Blockable) = {
    val st = synchronized {
      val st = state
      holdSchedule()
      state = state.withoutPublications
      st
    }

    for (v <- st.publications.reverseIterator) {
      t.awakeNonterminalValue(v)
    }

    st match {
      case CallInProgress(Nil) => { throw new AssertionError("Spurious check of call handle. " + this) }
      case CallInProgress(_) => {} // Already handled fully by parts above and below
      case CallHalted(_) => { t.halt() }
      case CallRaisedException(e) => { t.awakeException(e) } // t !! e
      case CallWasKilled => {}
    }

    synchronized {
      releaseSchedule()
    }
  }

  override def toString = synchronized {
    super.toString + s"($state)"
  }

}

/** Possible states of a CallHandle */
sealed abstract class CallState {
  val isFinal: Boolean
  // The publications that have not been read in reverse order
  val publications: Seq[AnyRef] = Nil

  def withoutPublications: CallState = this
}

sealed abstract class NonterminalCallState extends CallState { val isFinal = false }
case class CallInProgress(_publications: Seq[AnyRef]) extends NonterminalCallState {
  override val publications = _publications
  override def withoutPublications = copy(Nil)
}

sealed abstract class TerminalCallState extends CallState { val isFinal = true }
case class CallRaisedException(e: OrcException) extends TerminalCallState
case class CallHalted(_publications: Seq[AnyRef]) extends TerminalCallState {
  override val publications = _publications
  override def withoutPublications = copy(Nil)
}
case object CallWasKilled extends TerminalCallState
