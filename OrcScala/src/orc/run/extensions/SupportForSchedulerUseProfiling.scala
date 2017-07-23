//
// SupportForSchedulerUseProfiling.scala -- Scala class SupportForSchedulerUseProfiling
// Project OrcScala
//
// Created by amp on Jul 22, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.extensions

import orc.Schedulable
import java.lang.ThreadLocal
import orc.OrcRuntime
import scala.collection.JavaConverters._

trait SupportForSchedulerUseProfiling extends OrcRuntime {
  import SupportForSchedulerUseProfiling._
  
  private val counters = java.util.concurrent.ConcurrentHashMap.newKeySet[Counter]()
  private val counterThreadLocal = new ThreadLocal[Counter]() {
    override def initialValue() = {
      val c = new Counter()
      counters.add(c)
      c
    }
  }
  
  def increment(n: Int) = {
    counterThreadLocal.get().value += n
  }
  
  def count = {
    val cs = counters.asScala
    cs.map(_.value).sum
  }

  abstract override def stopScheduler(): Unit = {
    super.stopScheduler()
    Logger.info(s"scheduled Schedulables = $count")
  }

  abstract override def schedule(t: Schedulable): Unit = {
    increment(1)
    super.schedule(t)
  }

  abstract override def stage(ts: List[Schedulable]): Unit = {
    increment(ts.size)
    super.stage(ts)
  }

  // Schedule function is overloaded for convenience
  override def stage(t: Schedulable): Unit = {
    increment(1)
    super.stage(t)
  }
  override def stage(t: Schedulable, u: Schedulable): Unit = {
    increment(2)
    super.stage(t, u)
  }
  
}

object SupportForSchedulerUseProfiling {
  private final class Counter() {
    var value = 0
  }
}