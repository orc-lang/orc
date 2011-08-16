//
// SiteCallHandle.scala -- Scala class/trait/object SiteCallHandle
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
import orc.error.OrcException
import orc.Handle
import orc.CaughtEvent
import orc.OrcRuntime

/**
 * 
 *
 * @author dkitchin
 */
class SiteCallHandle(caller: Token, calledSite: AnyRef, actuals: List[AnyRef]) extends Handle with Blocker {

    val quiescentWhileBlocked = caller.runtime.quiescentWhileInvoked(calledSite)
    
    var listener: Option[Token] = Some(caller)
    var invocationThread: Option[Thread] = None
    
    caller.blockOn(this)

    def run() {
      try {
        synchronized {
          if (listener.isDefined) {
            invocationThread = Some(Thread.currentThread)
          } else {
            throw new InterruptedException()
          }
        }
        caller.runtime.invoke(this, calledSite, actuals)
      } catch {
        case e: OrcException => this !! e
        case e: InterruptedException => throw e
        case e: Exception => { notifyOrc(CaughtEvent(e)); halt() }
      }
    }

    def publish(v: AnyRef) =
      synchronized {
        listener foreach { _ publish v }
      }

    def halt() =
      synchronized {
        listener foreach { _.halt() }
      }

    def !!(e: OrcException) =
      synchronized {
        listener foreach { _ !! e }
      }

    def notifyOrc(event: orc.OrcEvent) =
      synchronized {
        listener foreach { _ notifyOrc event }
      }

    def isLive =
      synchronized {
        listener.isDefined
      }

    def kill() =
      synchronized {
        invocationThread foreach { _.interrupt() }
        listener = None
      }

}