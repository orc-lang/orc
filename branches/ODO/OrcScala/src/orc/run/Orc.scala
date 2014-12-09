//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.ast.oil.nameless.Expression
import orc.{ OrcRuntime, OrcExecutionOptions, OrcEvent }
import orc.run.core.{ Token, Execution }

abstract class Orc(val engineInstanceName: String) extends OrcRuntime {

  var roots = new java.util.concurrent.ConcurrentHashMap[Execution, Unit]

  def run(node: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    startScheduler(options: OrcExecutionOptions)

    /* Extend Execution to clean up the roots map when it halts or is killed.
     * This cannot be an event handler because only one handler is allowed to
     * handle any event.
     * 
     * In the past roots contained weak references. However this combined with
     * the OrcSiteCallTargets resulted in entire group trees being collected all
     * at once without actually halting, which makes the interpreter hang 
     * without exiting. 
     */
    val root = new Execution(node, options, k, this) {
      override def onHalt() = {
        roots.remove(root)
        super.onHalt()
      }

      override def kill() = {
        roots.remove(root)
        super.onHalt()
      }
    }
    installHandlers(root)

    roots.put(root, ())

    val t = new Token(node, root)
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
