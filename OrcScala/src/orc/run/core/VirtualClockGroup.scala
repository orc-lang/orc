//
// VirtualClockGroup.scala -- Scala class VirtualClockGroup
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jan 27, 2013.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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

  /* Block parent Vclock progress */
  parentClock foreach { _.unsetQuiescent() }

  override def toString = super.toString + s"(parentClock=$parentClock)"

  def publish(t: Token, v: Option[AnyRef]) {
    t.designateClock(parentClock)
    t.migrate(parent).publish(v)
  }

  def onHalt() = synchronized {
    /* Permit parent Vclock to progress */
    if (!isKilled) parentClock foreach { _.setQuiescent() }
    parent.remove(this)
  }
  def onDiscorporate() = synchronized {
    /* Permit parent Vclock to progress */
    // Note: A discorporated member is treated as quiescent.
    if (!isKilled) parentClock foreach { _.setQuiescent() }
    parent.discorporate(this)
  }

  override def kill() = synchronized {
    /* Permit parent Vclock to progress */
    if (!isKilled) parentClock foreach { _.setQuiescent() }
    super.kill()
  }

}
