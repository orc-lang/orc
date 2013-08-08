//
// Execution.scala -- Scala class Execution
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import java.util.logging.Level

import orc.{ PublishedEvent, OrcRuntime, OrcExecutionOptions, OrcEvent, HaltedOrKilledEvent, CaughtEvent }
import orc.ast.oil.nameless.Expression
import orc.error.runtime.TokenError
import orc.run.Logger

/** An execution is a special toplevel group,
  * associated with the entire program.
  *
  * @author dkitchin
  */
class Execution(
  private[run] var _node: Expression,
  private[run] var _options: OrcExecutionOptions,
  private var eventHandler: OrcEvent => Unit,
  override val runtime: OrcRuntime)
  extends Group {

  override val root = this

  val tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

  def node = _node;

  def options = _options;

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    notifyOrc(PublishedEvent(v.get))
    t.halt()
  }

  override def kill() = {
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
    super.kill()
  }

  def onHalt() {
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
  }

  def run() = { throw new AssertionError("Execution scheduled") }

  val oldHandler = eventHandler
  eventHandler = {
    case e @ CaughtEvent(je: Error) => { Logger.log(Level.SEVERE, "Unexpected Java Error thrown; killing Orc Execution", je); oldHandler(e); kill() }
    case e @ CaughtEvent(_: TokenError) => { oldHandler(e); kill() }
    case e => oldHandler(e)
  }

  def installHandler(newHandler: PartialFunction[OrcEvent, Unit]) = {
    val oldHandler = eventHandler
    eventHandler = { e => if (newHandler isDefinedAt e) newHandler(e) else oldHandler(e) }
  }

  def notifyOrc(event: OrcEvent) {
    try {
      eventHandler(event)
    } catch {
      case e: InterruptedException => throw e
      case e: Throwable => { Logger.log(Level.SEVERE, "Event handler abnormal termination", e); throw e }
    }
  }

}
