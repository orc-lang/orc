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

Console.println(this+".<init>: Blocking "+parentClock)
  /* Block parent Vclock progress */
  parentClock foreach { _.unsetQuiescent() }

  def publish(t: Token, v: Option[AnyRef]) {
Console.println(this+".publish: Resetting clock on "+t+" to "+parentClock/*+(if (parentClock.isDefined) " readyCount="+parentClock.get.readyCount else "")*/)
    t.designateClock(parentClock)
    t.migrate(parent).publish(v)
  }

  def onHalt() = synchronized {
    /* Permit parent Vclock to progress */
    if (!isKilled) { Console.println(this+".onHalt: Unblocking "+parentClock/*+(if (parentClock.isDefined) " readyCount="+parentClock.get.readyCount else "")*/); parentClock foreach { _.setQuiescent() } }
    parent.remove(this)
  }

  override def kill() = synchronized {
    /* Permit parent Vclock to progress */
    if (!isKilled) { Console.println(this+".kill: Unblocking "+parentClock/*+(if (parentClock.isDefined) " readyCount="+parentClock.get.readyCount else "")*/); parentClock foreach { _.setQuiescent() } }
    super.kill()
  }

//  override def add(m: GroupMember) = synchronized { super.add(m); Console.println(this+".add "+m+": size="+this.inhabitants.size) }
//  override def remove(m: GroupMember) = synchronized { super.remove(m); Console.println(this+".remove "+m+": size="+this.inhabitants.size) }

}
