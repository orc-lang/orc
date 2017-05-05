//
// Subgroup.scala -- Scala class Subgroup
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.{ CaughtEvent, OrcEvent }

/** @author dkitchin
  */
abstract class Subgroup(val parent: Group) extends Group {

  override val runtime = parent.runtime

  override val execution = parent.execution

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = super.checkAlive() && parent.checkAlive()

  parent.add(this)

  override def kill() = synchronized { super.kill(); parent.remove(this) }

  def notifyOrc(event: OrcEvent) = execution.notifyOrc(event)

  def run() {
    val beginProfInterval = orc.util.Profiler.beginInterval(0L, 'Subgroup_run)
    try {
      if (parent.isKilled()) { kill() }
    } catch {
      case e: InterruptedException => Thread.currentThread().interrupt()
      case e: Throwable => { notifyOrc(CaughtEvent(e)) }
    } finally {
      orc.util.Profiler.endInterval(0L, 'Token_run, beginProfInterval)
    }
  }

}
