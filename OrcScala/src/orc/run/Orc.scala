//
// Orc.scala -- Scala class Orc
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.ast.oil.nameless.Expression
import orc.{ OrcRuntime, OrcExecutionOptions, OrcEvent }
import orc.run.core.{ Token, Execution }
import scala.collection.mutable.SynchronizedSet
import scala.collection.mutable.HashSet
import scala.ref.WeakReference
import orc.run.core.EventHandler

abstract class Orc(val engineInstanceName: String) extends OrcRuntime {

  /** Execution groups (group tree roots) are tracked for monitoring/debugging */
  var roots = new java.util.concurrent.ConcurrentHashMap[WeakReference[Execution], Unit]

  def run(node: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    startScheduler(options: OrcExecutionOptions)

    val root = new Execution(node, options, k, this)
    installHandlers(root)

    roots.put(new WeakReference(root), ())

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
  def installHandlers(host: EventHandler) {}

}
