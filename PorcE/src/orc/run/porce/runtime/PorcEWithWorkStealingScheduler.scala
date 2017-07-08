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

import orc.run.Orc
import orc.run.extensions.SimpleWorkStealingScheduler
import orc.OrcExecutionOptions
import orc.Schedulable
import orc.run.porce.Logger
import java.util.logging.Level
import com.oracle.truffle.api.CompilerDirectives

/** An Orc runtime engine extension which
  * schedules Orc Tokens to run in an OrcThreadPoolExecutor.
  *
  * @author amp
  */
trait PorcEWithWorkStealingScheduler extends Orc {
  var scheduler: SimpleWorkStealingScheduler = null

  def startScheduler(options: OrcExecutionOptions): Unit = {
    val maxSiteThreads = if (options.maxSiteThreads > 0) options.maxSiteThreads else 256
    scheduler = new SimpleWorkStealingScheduler(maxSiteThreads) {
      override def beforeExecute(w: Worker, r: Schedulable): Unit = {
      }

      override def afterExecute(w: Worker, r: Schedulable, t: Throwable): Unit = {
        r.onComplete()
        if (t != null) {
          CompilerDirectives.transferToInterpreter()
          Logger.log(Level.WARNING, s"Schedulable threw exception.", t)
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

  def schedule(t: Schedulable): Unit = {
    // We do not check if scheduler is null because it will just throw an NPE and the check might decrease performance on a hot path.
    t.onSchedule()
    scheduler.schedule(t)
  }

  def stopScheduler(): Unit = {
    scheduler.stopScheduler()
  }
}