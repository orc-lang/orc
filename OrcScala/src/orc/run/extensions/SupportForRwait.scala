//
// SupportForRwait.scala -- Scala trait SupportForRwait
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.util.{ Timer, TimerTask }

import orc.{ CallContext, OrcEvent }
import orc.run.Orc
import orc.run.core.EventHandler
import orc.values.Signal

/** @author dkitchin
  */
case class RwaitEvent(delay: BigInt, callContext: CallContext) extends OrcEvent

trait SupportForRwait extends Orc {

  val timer: Timer = new Timer()

  override def stop() = { timer.cancel(); super.stop() }

  /* Note that all executions in a runtime with Rwait support
   * will share the same timer queue, so that we can stop the
   * timer when the runtime is shut down.
   */

  override def installHandlers(host: EventHandler) {
    val thisHandler = {
      case RwaitEvent(delay, callContext) => {
        val callback =
          new TimerTask() {
            @Override
            override def run() { callContext.publish(Signal) }
          }
        timer.schedule(callback, delay.toLong)
      }
    }: PartialFunction[OrcEvent, Unit]

    host.installHandler(thisHandler)
    super.installHandlers(host)
  }

}
