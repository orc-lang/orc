//
// SupportForSynchronousExecution.scala -- Scala trait SupportForSynchronousExecution
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.OrcEvent
import orc.HaltedOrKilledEvent
import orc.OrcExecutionOptions
import orc.ast.oil.nameless.Expression
import orc.error.runtime.ExecutionException
import orc.util.LatchingSignal

/**
  * @author dkitchin
  */
trait SupportForSynchronousExecution extends OrcRuntime {
  protected var runSyncThread: Thread = null

  /** Wait for execution to complete, rather than dispatching asynchronously.
    * The continuation takes only values, not events.
    */
  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  def runSynchronous(node: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    synchronized {
      if (runSyncThread != null) throw new IllegalStateException("runSynchronous on an engine that is already running synchronously")
      runSyncThread = Thread.currentThread()
    }
    val doneSignal = new LatchingSignal()
    def syncAction(event: OrcEvent): Unit = {
      event match {
        case HaltedOrKilledEvent => { doneSignal.signal() }
        case _ => {}
      }
      k(event)
    }
    try {
      this.run(node, syncAction, options)
      doneSignal.await()
    } finally {
      // Important: runSyncThread must be null before calling stop
      synchronized {
        runSyncThread = null
      }
      this.stop()
    }
  }

  /** If no continuation is given, discard published values and run silently to completion. */
  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  def runSynchronous(node: Expression, options: OrcExecutionOptions) {
    runSynchronous(node, { _: OrcEvent => }, options)
  }

  abstract override def stop() = {
    super.stop()
    synchronized {
      if (runSyncThread != null) runSyncThread.interrupt()
    }
  }

}
