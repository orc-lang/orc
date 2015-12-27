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

import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions, OrcRuntime, PublishedEvent }
import orc.run.core.{ Group, GroupMember, Token }

/** Proxy for a group the resides on a remote dOrc node.
  * RemoteGroupProxy is created locally when a token has been migrated from
  * another node. Notifications sent to this proxy from its members will be
  * passed to the remote group.  When all local group members halt, the remote
  * group will be notified.  If the remote group is killed, this proxy will
  * pass the kill on to the local members.
  *
  * @author jthywiss
  */
class RemoteGroupProxy(val runtime: OrcRuntime, val options: OrcExecutionOptions, sendEventFunc: OrcEvent => Unit) extends Group {

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = ???

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    Logger.entering(getClass.getName, "publish", Seq(t, v))
    notifyOrc(PublishedEvent(v.get))
    t.halt()
  }

  override def kill() = {
    Logger.entering(getClass.getName, "kill")
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
    super.kill()
  }

  def onHalt() {
    Logger.entering(getClass.getName, "onHalt")
    if (!isKilled) notifyOrc(HaltedOrKilledEvent)
  }

  def run() = throw new AssertionError("RemoteGroupProxy scheduled")

  def notifyOrc(event: OrcEvent) = {
    Logger.entering(getClass.getName, "notifyOrc", Seq(event))
    sendEventFunc(event)
    Logger.exiting(getClass.getName, "notifyOrc")
  }

}


/** Proxy for one or more remote members of a group (tokens and groups).
  * RemoteGroupMembersProxy is created when a token is migrated to another node.
  * As the migrated token continues to execute, this proxy may come to represent
  * multiple tokens, and/or subgroups.  Those remote group members will send
  * notifications to the parent group via this proxy.  When all remote members
  * halt, this proxy will remove itself from the parent.  If this proxy is
  * killed, it will pass the kill on to its remote members.
  *
  * @author jthywiss
  */
class RemoteGroupMembersProxy(val parent: Group, sendKillFunc: () => Unit, val proxyId: LeaderRuntime#GroupProxyId) extends GroupMember {
  override val nonblocking = true

  private var alive = true

  def kill() = synchronized {
    if (alive) {
      alive = false
      sendKillFunc()
      Logger.finer(s"RemoteGroupMembersProxy.kill: parent.members=${parent.members}")
      parent.remove(this)
    }
  }

  def suspend() = ???
  def resume() = ???

  def notifyOrc(event: OrcEvent) = parent.notifyOrc(event)

  /** An expensive walk-to-root check for alive state */
  def checkAlive(): Boolean = synchronized { alive } && parent.checkAlive()

  def run() = throw new AssertionError("RemoteGroupMembersProxy scheduled")
}
