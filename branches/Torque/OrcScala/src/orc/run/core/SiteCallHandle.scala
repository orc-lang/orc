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
 * A call handle specific to site calls.
 * Scheduling this call handle will invoke the site.
 *
 * @author dkitchin
 */
class SiteCallHandle(caller: Token, calledSite: AnyRef, actuals: List[AnyRef]) extends CallHandle(caller) {

  var invocationThread: Option[Thread] = None
  
  val quiescentWhileBlocked = caller.runtime.quiescentWhileInvoked(calledSite)    
  
  def run() {
    try {
      if (synchronized {
        if (isLive) {
          invocationThread = Some(Thread.currentThread)
        } 
        isLive
      }) 
      {
        caller.runtime.invoke(this, calledSite, actuals)  
      }
    } catch {
      case e: OrcException => this !! e
      case e: InterruptedException => halt() // Thread interrupt causes halt without notify
      case e: Exception => { notifyOrc(CaughtEvent(e)); halt() }
    } finally {
      synchronized {
        invocationThread = None
      }
    }
  }
  
  
  override def kill() {
    synchronized {
      super.kill()
      invocationThread foreach { _.interrupt() }
    }
  }

}
