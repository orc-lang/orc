//
// ThreadPoolScheduler.scala -- Scala trait ThreadPoolScheduler
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Mar 29, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc

import scala.actors.scheduler.ResizableThreadPoolScheduler
import scala.concurrent.ManagedBlocker

/**
 * An Orc runtime engine extension which
 * schedules Orc Tokens to run in a ThreadPoolExecutor, using
 * Scala's ResizableThreadPoolScheduler.
 *
 * @author jthywiss
 */
trait ThreadPoolScheduler extends Orc {
  
  type ActorScheduler = scala.actors.IScheduler { def start(): Unit; def snapshot(): Unit }

  /**
   * @return the Scala Actor Scheduler that contains the ThreadPoolExecutor
   *    to use for this token scheduler.
   */
  protected def newActorScheduler(): ActorScheduler = new ResizableThreadPoolScheduler(true, false)

  /**
   * Some schedulers, such as ForkJoinScheduler, need blocking tasks wrapped in
   * a ManagedBlocker call.  Most do not.
   */
  protected val useManagedBlocker = false

  private val scalaActorScheduler = newActorScheduler()
  scalaActorScheduler.start()

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(scala.collection.immutable.List)
   */
  override def schedule(ts: List[Token]) {
    ts.foreach(schedule(_))
  }

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(orc.OrcRuntime.Token, orc.OrcRuntime.Token)
   */
  override def schedule(t: Token, u: Token) {
    schedule(t)
    schedule(u)
  }

  /* (non-Javadoc)
   * @see orc.OrcRuntime#schedule(orc.OrcRuntime.Token)
   */
  override def schedule(t: Token) {
    if (useManagedBlocker) {
      scalaActorScheduler.execute(new Runnable with ManagedBlocker {
        def run() { scalaActorScheduler.managedBlock(this) }
        var isReleasable = false
        def block() = {
          t.run()
          isReleasable = true
          true
        }
      })
    } else {
      scalaActorScheduler.execute(t)
    }
  }

  /* (non-Javadoc)
   * @see orc.run.Orc#stop()
   */
  override def stop = {
    scalaActorScheduler.snapshot()  // Interrupts running worker threads
    scalaActorScheduler.shutdown()
    super.stop
  }

}