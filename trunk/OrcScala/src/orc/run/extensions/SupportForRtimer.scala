//
// SupportForRtimer.scala -- Scala trait SupportForRtimer
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.Handle
import orc.OrcEvent
import orc.OrcOptions
import java.util.Timer
import java.util.TimerTask
import orc.ast.oil.nameless.Expression

/**
 * 
 *
 * @author dkitchin
 */

case class RtimerEvent(delay: BigInt, caller: Handle) extends OrcEvent

trait SupportForRtimer extends Orc {
  
  val timer: Timer = new Timer()
  
  override def stop = { timer.cancel() ; super.stop }
  
  class Execution(_node: Expression, k: OrcEvent => Unit, _options: OrcOptions) extends super.Execution(_node,k,_options) {
    override def notify(event: OrcEvent) {
      event match {
        case RtimerEvent(delay, caller) => {
          val callback =  
            new TimerTask() {
              @Override
              override def run() { caller.publish() }
            }
          timer.schedule(callback, delay.toLong)
        }
        case _ => super.notify(event)
      }
    }
  }
  
}