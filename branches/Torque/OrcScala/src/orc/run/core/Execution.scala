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
    k: OrcEvent => Unit, 
    private[run] var _options: OrcExecutionOptions,
    val runtime: OrcRuntime) 
extends Group {

  def node = _node;
  def options = _options;

  val tokenCount = new java.util.concurrent.atomic.AtomicInteger(0);

  def publish(t: Token, v: AnyRef) {
    synchronized {
      k(PublishedEvent(v))
      t.halt()
    }
  }

  def onHalt() {
    k(HaltedEvent)
  }

  val eventHandler: OrcEvent => Unit = {
    val handlers = runtime.asInstanceOf[Orc].generateOrcHandlers(this)
    val baseHandler = { case e => k(e) }: PartialFunction[OrcEvent, Unit]
    def composeOrcHandlers(f: PartialFunction[OrcEvent, Unit], g: PartialFunction[OrcEvent, Unit]) = f orElse g
    handlers.foldRight(baseHandler)(composeOrcHandlers)
  }

  def notifyOrc(event: OrcEvent) = { eventHandler(event) }

  val root = this

}