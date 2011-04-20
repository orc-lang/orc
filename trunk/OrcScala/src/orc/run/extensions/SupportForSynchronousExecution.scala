//
// SupportForSynchronousExecution.scala -- Scala trait SupportForSynchronousExecution
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.OrcEvent
import orc.HaltedEvent
import orc.OrcExecutionOptions
import orc.ast.oil.nameless.Expression
import orc.error.runtime.ExecutionException

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForSynchronousExecution extends OrcRuntime {
  
  /**
   * Wait for execution to complete, rather than dispatching asynchronously.
   * The continuation takes only values, not events.
   */
  @throws(classOf[ExecutionException])
  def runSynchronous(node: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    val done: scala.concurrent.SyncVar[Unit] = new scala.concurrent.SyncVar()
    def syncAction(event: OrcEvent): Unit = {
      event match {
        case HaltedEvent => { done.set({}) }
        case _ => { }
      }
      k(event)
    }
    this.run(node, syncAction, options)
    done.get
  }

  /** If no continuation is given, discard published values and run silently to completion. */
  @throws(classOf[ExecutionException])
  def runSynchronous(node: Expression, options: OrcExecutionOptions) {
    runSynchronous(node, { _: OrcEvent => }, options)
  }
 
}
