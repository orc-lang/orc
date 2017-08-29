//
// CallController.scala -- Scala class CallController
// Project OrcScala
//
// Created by dkitchin on Aug 26, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.CallContext
import orc.error.OrcException

/** An abstract call controller for any call made by a token.
  *
  * All descendants of CallController must maintain a scheduling invariant:
  * it must not be possible for the controller to reschedule the calling token
  * until the calling thread enters the onComplete method of the token.
  *
  * SiteCallController maintains this invariant by staging itself on the caller,
  * so that it cannot be scheduled to run until after the caller has completed.
  *
  * AwaitCallController maintains this invariant because the calling token keeps
  * its governing clock from becoming quiescent until the token itself becomes
  * quiescent in its onComplete method.
  *
  * @author dkitchin
  */
abstract class CallController(val caller: Token) extends CallContext with Blocker {
  // This is the only Blocker that can produce exceptions.

  protected var state: CallState = CallState.InProgress(List())
  protected var quiescent = false

  // a mechanism to delay schedule of caller until later without holding the lock.
  var scheduleHeld = false
  var scheduleOnRelease = false

  /** Set the CallController to hold scheduling the caller until releaseSchedule is called.
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
      caller.runtime.schedule(caller)
  }

  /** Allow scheduling the caller again and possibly schedule it.
    *
    * Call inside a synchronized block only.
    */
  private def releaseSchedule() = {
    assert(scheduleHeld)
    if (scheduleOnRelease)
      caller.runtime.schedule(caller)
    scheduleHeld = false
    scheduleOnRelease = false
  }

  /* Returns true if the state transition was made,
   * false otherwise (e.g. if the controller was already in a final state)
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
    setState(CallState.InProgress(v +: state.publications))
  }
  def halt() = synchronized {
    // A second call to this can occur because we are trying to halt a controller for an OrcSite call.
    // And it's idempotent, so it shouldn't matter.
    //assert(state.isInstanceOf[CallState.InProgress] || state == CallState.WasKilled, state.toString)
    setState(CallState.Halted(state.publications))
  }
  def discorporate() = synchronized {
    assert(state.isInstanceOf[CallState.InProgress] || state == CallState.WasKilled, state.toString)
    caller.discorporate()
  }
  def halt(e: OrcException) = synchronized {
    setState(CallState.RaisedException(e))
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
      case CallState.InProgress(_) if !quiescent => {
        quiescent = true
        caller.setQuiescent()
      }
      case CallState.WasKilled => {}
      case _ => throw new AssertionError(s"CallController.setQuiescent in bad state: state=$state, quiescent=$quiescent")
    }
  }

  def isLive = synchronized { !state.isFinal }

  def kill(): Unit = synchronized {
    setState(CallState.WasKilled)
  }

  def check(t: Blockable): Unit = orc.util.Profiler.measureInterval(0L, 'CallController_check) {
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
      case CallState.InProgress(Nil) => { throw new AssertionError("Spurious check of call controller. " + this) }
      case CallState.InProgress(_) => {} // Already handled fully by parts above and below
      case CallState.Halted(_) => { t.halt() }
      case CallState.RaisedException(e) => { t.awakeException(e) } // t !! e
      case CallState.WasKilled => {}
    }

    synchronized {
      releaseSchedule()
    }
  }

  override def toString = synchronized {
    super.toString + s"($state)"
  }

}

/** Possible states of a CallController */
protected abstract sealed class CallState() {
  val isFinal: Boolean
  // The publications that have not been read in reverse order
  val publications: Seq[AnyRef] = Nil

  def withoutPublications: CallState = this
}

protected object CallState {
  abstract sealed class NonterminalCallState() extends CallState() { val isFinal = false }
  case class InProgress(_publications: Seq[AnyRef]) extends NonterminalCallState() {
    override val publications = _publications
    override def withoutPublications = copy(Nil)
  }

  abstract sealed class TerminalCallState extends CallState() { val isFinal = true }
  case class RaisedException(e: OrcException) extends TerminalCallState()
  case class Halted(_publications: Seq[AnyRef]) extends TerminalCallState() {
    override val publications = _publications
    override def withoutPublications = copy(Nil)
  }
  case object WasKilled extends TerminalCallState()
}
