//
// SupportForSynchronousExecution.scala -- Scala class/trait/object SupportForSynchronousExecution
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.OrcEvent
import orc.Publication
import orc.Halted
import orc.oil.nameless.Expression


/**
 * 
 *
 * @author dkitchin
 */
trait SupportForSynchronousExecution extends OrcRuntime {
  
  /* Wait for execution to complete, rather than dispatching asynchronously.
   * The continuation takes only values, not events.
   */
  def runSynchronous(node: Expression, k: AnyRef => Unit) {
    val done: scala.concurrent.SyncVar[Unit] = new scala.concurrent.SyncVar()
    def ksync(event: OrcEvent): Unit = {
      event match {
        case Publication(v) => k(v)
        case Halted => { done.set({}) }
      }
    }
    this.run(node, ksync)
    done.get
  }
  
    /* If no continuation is given, discard published values and run silently to completion. */
  def runSynchronous(node: Expression) {
    runSynchronous(node, { v: AnyRef => { /* suppress publications */ } })
  }
 
}