//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.{ ExecutionRoot, OrcEvent, OrcExecutionOptions, OrcRuntime }
import orc.ast.oil.nameless.Expression
import orc.run.core.{ Execution, Token }

abstract class Orc(val engineInstanceName: String) extends OrcRuntime {
  thisruntime =>

  val roots = java.util.concurrent.ConcurrentHashMap.newKeySet[ExecutionRoot]()

  override def removeRoot(root: ExecutionRoot) = roots.remove(root)

  def run(node: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions){
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
  def installHandlers(host: Execution) {}

}
