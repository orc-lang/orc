//
// VirtualClockGroup.scala -- Scala class VirtualClockGroup
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jan 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

/** Stop the parent virtual clock while this group is live, and "pop"
  * the clock reference on published tokens.
  *
  * @author jthywiss
  */
class VirtualClockGroup(val parentClock: Option[VirtualClock], parent: Group) extends Subgroup(parent) {

  /* Block Vclock progress */
  parentClock foreach { _.unsetQuiescent() }

  def publish(t: Token, v: Option[AnyRef]) {
    t.designateClock(parentClock)
    t.migrate(parent).publish(v)
  }

  def onHalt() {
    parent.remove(this)
    /* Permit Vclock to progress */
    parentClock foreach { _.setQuiescent() }
  }

  override def kill() {
    super.kill();
    /* Permit Vclock to progress */
  }

}
