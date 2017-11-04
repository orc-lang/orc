//
// PorcEWithWorkStealingScheduler.scala -- A work-stealing scheduler for PorcE
// Project OrcScala
//
// Created by amp on Feb, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.logging.Level

import com.oracle.truffle.api.CompilerDirectives

import orc.OrcExecutionOptions
import orc.Schedulable
import orc.run.Orc
import orc.run.extensions.SimpleWorkStealingScheduler
import orc.run.porce.Logger
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/** An Orc runtime engine extension which schedules Orc Tokens to
  * run in an OrcThreadPoolExecutor.
  *
  * WARNING: PorcEWithWorkStealingScheduler does not call onSchedule or
  * onComplete.
  *
  * @author amp
  */
trait PorcEWithWorkStealingScheduler extends Orc {
  var scheduler: SimpleWorkStealingScheduler = null

  def beforeExecute(): Unit

  def startScheduler(options: OrcExecutionOptions): Unit = {
    val maxSiteThreads = if (options.maxSiteThreads > 0) options.maxSiteThreads else 256
    scheduler = new SimpleWorkStealingScheduler(maxSiteThreads) {
      override def beforeExecute(w: Worker, r: Schedulable): Unit = {
        PorcEWithWorkStealingScheduler.this.beforeExecute()
      }

      override def afterExecute(w: Worker, r: Schedulable, t: Throwable): Unit = {
        //r.onComplete()
        if (t != null) {
          CompilerDirectives.transferToInterpreter()
          Logger.log(Level.SEVERE, s"Schedulable threw exception: $r", t)
        }
      }
    }

    scheduler.startScheduler()
  }

  def stage(ts: List[Schedulable]): Unit = {
    throw new UnsupportedOperationException("Stage not supported in PorcE");
  }
  override def stage(t: Schedulable): Unit = {
    throw new UnsupportedOperationException("Stage not supported in PorcE");
  }
  override def stage(a: Schedulable, b: Schedulable): Unit = {
    throw new UnsupportedOperationException("Stage not supported in PorcE");
  }

  @TruffleBoundary(allowInlining = true) @noinline
  def schedule(t: Schedulable): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    //t.onSchedule()
    scheduler.schedule(t)
  }

  def stopScheduler(): Unit = {
    if (scheduler != null)
      scheduler.stopScheduler()
  }
}