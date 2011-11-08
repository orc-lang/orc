//
// SiteCallHandle.scala -- Scala class SiteCallHandle
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

import orc.{Schedulable, CaughtEvent}
import orc.error.OrcException

/** A call handle specific to site calls.
  * Scheduling this call handle will invoke the site.
  *
  * @author dkitchin
  */
class SiteCallHandle(caller: Token, calledSite: AnyRef, actuals: List[AnyRef]) extends CallHandle(caller) with Schedulable {

  var invocationThread: Option[Thread] = None

  val governingClock = {
    if (caller.runtime.quiescentWhileInvoked(calledSite)) {
      None
    } else {
      caller.getClock()
    }
  }

  def run() {
    try {
      if (synchronized {
        if (isLive) {
          invocationThread = Some(Thread.currentThread)
        }
        isLive
      }) {
        caller.runtime.invoke(this, calledSite, actuals)
      }
    } catch {
      case e: OrcException => this !! e
      case e: InterruptedException => { halt(); Thread.currentThread().interrupt() } // Thread interrupt causes halt without notify
      case e: Exception => { notifyOrc(CaughtEvent(e)); halt() }
    } finally {
      synchronized {
        invocationThread = None
      }
    }
  }

  /* When a site call handle is scheduled, notify its clock accordingly. */
  override def onSchedule() {
    governingClock foreach { _.unsetQuiescent() }
  }

  /* NOTE: We do not override onComplete. A site call is not 'complete' until
   * the listener token is reawakened. Instead, we override the setState method.
   */
  override def setState(newState: CallState): Boolean = {
    synchronized {
      val success = super.setState(newState)
      /* If a successful transition was made, 
       * and the resulting state is final,
       * notify the clock.
       */
      if (success && !isLive) {
        governingClock foreach { _.setQuiescent() }
      }
      success
    }
  }

  override def kill() {
    synchronized {
      super.kill()
      invocationThread foreach { _.interrupt() }
    }
  }

}
