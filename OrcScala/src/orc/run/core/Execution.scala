//
// Execution.scala -- Scala class Execution
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

import java.util.logging.Level

import orc.{PublishedEvent, OrcRuntime, OrcExecutionOptions, OrcEvent, HaltedEvent, CaughtEvent}
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
  val runtime: OrcRuntime)
  extends Group {

  def node = _node;
  def options = _options;

  val tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

  def publish(t: Token, v: AnyRef) {
    synchronized {
      notifyOrc(PublishedEvent(v))
      t.halt()
    }
  }

  def onHalt() {
    notifyOrc(HaltedEvent)
  }

  def run() = { /* Do nothing. */ }

  val oldHandler = eventHandler
  eventHandler = {
    case e @ CaughtEvent(je: Error) => { Logger.log(Level.SEVERE, "Java Error: Stopping Orc runtime", je); runtime.stop; oldHandler(e) }
    case e @ CaughtEvent(_: TokenError) => { kill(); oldHandler(e) }
    case e => oldHandler(e)
  }

  def installHandler(newHandler: PartialFunction[OrcEvent, Unit]) = {
    val oldHandler = eventHandler
    eventHandler = { e => if (newHandler isDefinedAt e) { newHandler(e) } else { oldHandler(e) } }
  }

  def notifyOrc(event: OrcEvent) = { eventHandler(event) }

  val root = this

}
