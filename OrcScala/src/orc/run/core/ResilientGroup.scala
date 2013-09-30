//
// LimitGroup.scala -- Group representing limits in Orc 5C
// Project OrcScala
//
// $Id: LimitGroup.scala 3313 2013-09-26 02:38:29Z arthur.peters@gmail.com $
//
// Created by amp on Sep 24, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.run.Logger
import orc.OrcEvent
import orc.CaughtEvent

/** A ResilientGroup is the group associated with the expression resilient(e).
  * It overrides the kill method and prevents it's children from being killed.
  *
  * @author amp
  */
class ResilientGroup(private val parent: Group) extends Group {
  var wasKilled = false
  
  override val runtime = parent.runtime

  override val root = parent.root
  
  parent.add(this)
  root.add(this)

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    if (wasKilled) {
      t.halt()
    } else {
      t.migrate(parent)
      t.publish(v)
    }
  }

  def onHalt() = synchronized {
    parent.remove(this)
    root.remove(this)
  }

  override def kill() = synchronized {
    wasKilled = true
    parent.remove(this)
  }

  override def checkAlive(): Boolean = !isKilled()

  def notifyOrc(event: OrcEvent) = synchronized { parent }.notifyOrc(event)

  def run() {
    try {
      if (synchronized { parent.isKilled() }) { kill() }
    } catch {
      case e: InterruptedException => Thread.currentThread().interrupt()
      case e: Throwable => { notifyOrc(CaughtEvent(e)) }
    }
  }

  override def toString = s"${super.toString}($parent, $root, $wasKilled)"
}

