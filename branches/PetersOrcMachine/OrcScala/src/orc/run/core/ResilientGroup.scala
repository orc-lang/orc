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

/** A ResilientGroup is the group associated with the expression resilient(e).
  * It overrides the kill method and prevents it's children from being killed.
  *
  * @author amp
  */
class ResilientGroup(private var parent: Group) extends Subgroup(parent) {
  var wasKilled = false
 
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    //Logger.info(s"$this publishing $v from $t ($wasKilled)")
    if (wasKilled) {
      t.halt()
    } else {
      t.migrate(parent)
      t.publish(v)
    }
  }

  def onHalt() = synchronized {
    //Logger.info(s"$this halted. $wasKilled")
    parent.remove(this)
  }

  override def kill() = synchronized {
    //Logger.info(s"$this being killed. Marking and continuing.")
    wasKilled = true
    root.add(this)
    parent.remove(this)
    parent = root
  }

  override def checkAlive(): Boolean = !isKilled()
}