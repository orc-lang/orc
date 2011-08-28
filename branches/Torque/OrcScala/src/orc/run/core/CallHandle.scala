//
// CallHandle.scala -- Scala class/trait/object CallHandle
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

/**
 * 
 * An abstract call handle for any call made by a token.
 *
 * @author dkitchin
 */
abstract class CallHandle(val caller: Token) extends Handle with Blocker {

  protected var state: CallState = CallInProgress
  
  protected def setState(newState: CallState) {
    synchronized {
      if (isLive) { 
        state = newState
        caller.schedule() 
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

  def isLive = synchronized { state.isLive }

  def kill() {
    synchronized {
      state = CallWasKilled
    }
  }
  
  def check(t: Token) {
    synchronized {
      state match {
        case CallInProgress => { throw new AssertionError("Spurious check") }
        case CallReturnedValue(v) => { t.publish(v) }
        case CallSilent => {  t.halt() }
        case CallRaisedException(e) => { t !! e }
        case CallWasKilled => { throw new AssertionError("Spurious check") }
      }
    }
  }
  
}

/** Possible states of a CallHandle */
trait CallState { val isLive: Boolean = false }

case object CallInProgress extends CallState { override val isLive = true }
case class CallReturnedValue(v: AnyRef) extends CallState
case object CallSilent extends CallState
case class CallRaisedException(e: OrcException) extends CallState
case object CallWasKilled extends CallState
