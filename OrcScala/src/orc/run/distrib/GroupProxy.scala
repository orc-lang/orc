//
// GroupProxy.scala -- Scala classes RemoteGroupProxy and RemoteGroupMembersProxy
// Project OrcScala
//
// Created by jthywiss on Dec 25, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.{ HaltedOrKilledEvent, OrcEvent, PublishedEvent }
import orc.run.core.{ Execution, Group, GroupMember, Token }
import orc.CaughtEvent

/** Proxy for a group the resides on a remote dOrc node.
  * RemoteGroupProxy is created locally when a token has been migrated from
  * another node. Notifications sent to this proxy from its members will be
  * passed to the execution.  When all local group members halt, the remote
  * group will be notified.  If the remote group is killed, this proxy will
  * pass the kill on to the local members.
  *
  * @author jthywiss
  */
class RemoteGroupProxy(val execution: Execution, val remoteProxyId: DOrcExecution#GroupProxyId, pubFunc: (Token, Option[AnyRef]) => Unit, onHaltFunc: () => Unit) extends Group {

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = ???

  override def publish(t: Token, v: Option[AnyRef]) = synchronized {
    Logger.entering(getClass.getName, "publish", Seq(t, v))
    pubFunc(t, v)
    t.halt() //FIXME: Should we halt or just remove?
  }

  override def kill() = {
    Logger.entering(getClass.getName, "kill")
    /* All RemoteGroupProxy kills come from the remote site */
    super.kill()
  }

  override def onHalt() {
    Logger.entering(getClass.getName, "onHalt")
    if (!isKilled) onHaltFunc()
  }

  override def run() = throw new AssertionError("RemoteGroupProxy scheduled")

  override def notifyOrc(event: OrcEvent) = {
    Logger.entering(getClass.getName, "notifyOrc", Seq(event))
    execution.notifyOrc(event)
    Logger.exiting(getClass.getName, "notifyOrc")
  }

}

/** Proxy for one or more remote members of a group (tokens and groups).
  * RemoteGroupMembersProxy is created when a token is migrated to another node.
  * As the migrated token continues to execute, this proxy may come to represent
  * multiple tokens, and/or subgroups.  When all remote members halt, this
  * proxy will remove itself from the parent.  If this proxy is killed, it will
  * pass the kill on to its remote members.
  *
  * @author jthywiss
  */
class RemoteGroupMembersProxy(val parent: Group, sendKillFunc: () => Unit, val thisProxyId: DOrcExecution#GroupProxyId) extends GroupMember {
  override val nonblocking = true

  private var alive = true

  /** Remote group members all halted, so halt this. */
  def halt() = synchronized {
    if (alive) {
      alive = false
      parent.remove(this)
    }
  }

  /** The group is killing its members; pass it on. */
  override def kill() = synchronized {
    if (alive) {
      alive = false
      sendKillFunc()
      Logger.finer(s"RemoteGroupMembersProxy.kill: parent.members=${parent.members}")
      parent.remove(this)
    }
  }

  override def suspend() = ??? /* Send suspend cmd to remote */
  override def resume() = ??? /* Send resume cmd to remote */

  override def notifyOrc(event: OrcEvent) = parent.notifyOrc(event)

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = synchronized { alive } && parent.checkAlive()

  def run() {
    try {
      if (parent.isKilled()) { kill() }
    } catch {
      case e: InterruptedException => Thread.currentThread().interrupt()
      case e: Throwable => { notifyOrc(CaughtEvent(e)) }
    }
  }
}
