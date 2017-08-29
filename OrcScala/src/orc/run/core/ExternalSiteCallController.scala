//
// ExternalSiteCallController.scala -- Scala class ExternalSiteCallController
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.Schedulable
import orc.error.OrcException
import orc.values.sites.{ Delay, Site }

/** A call controller specific to site calls.
  * Scheduling this call controller will invoke the site.
  *
  * @author dkitchin
  */
class ExternalSiteCallController(caller: Token, calledSite: AnyRef, actuals: List[AnyRef]) extends CallController(caller) with Schedulable {
  override val nonblocking = calledSite match {
    case s: Site =>
      s.timeToHalt == Delay.NonBlocking
    case _ => false
  }

  var invocationThread: Option[Thread] = None

  def run() {
    val beginProfInterval = orc.util.Profiler.beginInterval(0L, 'ExternalSiteCallController_run)
    try {
      if (synchronized {
        if (isLive) {
          invocationThread = Some(Thread.currentThread)
        }
        isLive
      }) {
        val args = actuals.toArray
        try {
          caller.runtime.getInvoker(calledSite, args).invoke(this, calledSite, args)
        } catch {
          // FIXME: What is this check doing here and how will it handle OrcExceptions correctly.
          case e: Exception => { notifyOrc(CaughtEvent(new JavaException(e))); halt() }
        }
      }
    } catch {
      case e: OrcException => this.halt(e)
      case e: InterruptedException => { halt(); Thread.currentThread().interrupt() } // Thread interrupt causes halt without notify
      case e: Exception => { notifyOrc(CaughtEvent(e)); halt() }
    } finally {
      synchronized {
        invocationThread = None
      }
      orc.util.Profiler.endInterval(0L, 'ExternalSiteCallController_run, beginProfInterval)
    }
  }

  /* When a site call controller is scheduled, notify its clock accordingly. */
  override def onSchedule() {
    caller.getClock() foreach { _.unsetQuiescent() }
  }

  /* NOTE: We do NOT setQuiescent in onComplete. A site call is not
   * "complete" until the caller token is reawakened. Completion of
   * SiteCallController.run() indicates the call has been invoked, but
   * the call may continue to be outstanding.  Instead, we override
   * the setState method to look for completion of the site call.
   */

  override def setState(newState: CallState): Boolean = synchronized {
    val success = super.setState(newState)
    /* If a successful transition was made,
     * and the resulting state is final,
     * notify the clock.
     */
    if (success && !isLive) {
      caller.getClock() foreach { _.setQuiescent() }
    }
    success
  }

  override def kill() = synchronized {
    super.kill()
    invocationThread foreach { _.interrupt() }
  }

}
