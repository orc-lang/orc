//
// TrimGroup.scala -- Group representing limits in Orc 5C
// Project OrcScala
//
// $Id$
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

/** A TrimGroup is the group associated with the expression {| e |}
  *
  * @author amp
  */
class TrimGroup(parent: Group) extends Subgroup(parent) {
  var isFinished = false

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    if (!isFinished) {
      isFinished = true;

      t.migrate(parent)
      t.publish(v)
      this.kill()
    } else {
      t.kill()
    }
  }

  def onHalt() = synchronized {
    if (!isFinished)
      parent.remove(this)
  }
}