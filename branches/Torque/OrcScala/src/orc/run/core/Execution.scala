//
// Execution.scala -- Scala class/trait/object Execution
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

import orc.ast.oil.nameless.Expression
import orc.HaltedEvent
import orc.OrcEvent
import orc.OrcExecutionOptions
import orc.OrcRuntime
import orc.PublishedEvent
import orc.run.Orc

/**
 * 
 *
 * @author dkitchin
 */
/**
 * An execution is a special toplevel group,
 * associated with the entire program.
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
  
  def installHandler(newHandler: PartialFunction[OrcEvent, Unit]) = {
    val oldHandler = eventHandler
    eventHandler = { e => if (newHandler isDefinedAt e) { newHandler(e) } else { oldHandler(e) }}
  }

  def notifyOrc(event: OrcEvent) = { eventHandler(event) }

  val root = this

}