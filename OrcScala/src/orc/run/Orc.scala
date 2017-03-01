//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.{ ExecutionRoot, OrcEvent, OrcExecutionOptions, OrcRuntime }
import orc.ast.oil.nameless.Expression
import orc.run.core.{ EventHandler, Execution, Token }

abstract class Orc(val engineInstanceName: String) extends OrcRuntime {
  thisruntime =>

  val roots = java.util.concurrent.ConcurrentHashMap.newKeySet[ExecutionRoot]()

  override def removeRoot(root: ExecutionRoot) = roots.remove(root)

  def run(node: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions) {
    startScheduler(options: OrcExecutionOptions)

    val execution = new Execution(node, options, eventHandler, this)
    roots.add(execution)
    installHandlers(execution)

    val t = new Token(node, execution)
    schedule(t)
  }

  def stop() = {
    stopScheduler()
  }

  /** Add all needed event handlers to an execution.
    * Traits which add support for more events will override this
    * method and introduce more handlers.
    */
  def installHandlers(host: EventHandler) {}

  /** A thread ID 32-bit integer that can be combined with a thread local
    * counter to produce identifiers.
    *
    * WARNING: Uniqueness is attempted, but not guaranteed.  Indicative only,
    * for non-critical uses, such as debugging log/trace.
    *
    * We use the least significant 32 bits of Java's thread ID by default.
    * Some Runtimes will override this with a more detailed thread ID.
    */
  def runtimeDebugThreadId = Thread.currentThread().getId.toInt

}
