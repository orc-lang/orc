//
// GroupProxy.scala -- Scala classes RemoteGroupProxy and RemoteGroupMembersProxy, and trait GroupProxyManager
// Project OrcScala
//
// Created by jthywiss on Dec 25, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.{ CaughtEvent, OrcEvent }
import orc.run.core.{ Execution, Group, GroupMember, Token }
import orc.Schedulable

/** Proxy for a group the resides on a remote dOrc node.
  * RemoteGroupProxy is created locally when a token has been migrated from
  * another node. Notifications sent to this proxy from its members will be
  * passed to the execution.  When all local group members halt, the remote
  * group will be notified.  If the remote group is killed, this proxy will
  * pass the kill on to the local members.
  *
  * @author jthywiss
  */
class RemoteGroupProxy(override val execution: Execution, 
    val remoteProxyId: DOrcExecution#GroupProxyId, 
    pubFunc: (Token, Option[AnyRef]) => Unit, 
    onHaltFunc: () => Unit, 
    onDiscorporateFunc: () => Unit) extends Group {

  /** An expensive walk-to-root check for alive state */
  override def checkAlive(): Boolean = ???

  override def publish(t: Token, v: Option[AnyRef]) = synchronized {
    //Logger.entering(getClass.getName, "publish", Seq(t, v))
    pubFunc(t, v)
    t.halt()
  }

  override def kill() = {
    //Logger.entering(getClass.getName, "kill")
    /* All RemoteGroupProxy kills come from the remote side */
    super.kill()
  }

  override def onHalt() {
    //Logger.entering(getClass.getName, "onHalt")
    if (!isKilled) onHaltFunc()
  }

  override def onDiscorporate() {
    //Logger.entering(getClass.getName, "onDiscorporate")
    if (!isKilled) onDiscorporateFunc()
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
    //Logger.entering(getClass.getName, "halt")
    if (alive) {
      alive = false
      parent.remove(this)
    }
  }

  /** Remote group members all halted, but there was discorporates, so discorporate this. */
  def discorporate() = synchronized {
    //Logger.entering(getClass.getName, "discorporate")
    if (alive) {
      alive = false
      parent.discorporate(this)
    }
  }

  /** The group is killing its members; pass it on. */
  override def kill() = synchronized {
    //Logger.entering(getClass.getName, "kill")
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
    //Logger.entering(getClass.getName, "run")
    try {
      if (parent.isKilled()) { kill() }
    } catch {
      case e: InterruptedException => Thread.currentThread().interrupt()
      case e: Throwable => { notifyOrc(CaughtEvent(e)) }
    }
  }
}

/** A mix-in to manage proxied groups.
  *
  * @author jthywiss
  */
trait GroupProxyManager { self: DOrcExecution =>
  type GroupProxyId = Long

  protected val proxiedGroups = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupProxy]
  protected val proxiedGroupMembers = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupMembersProxy]

  def sendToken(token: Token, destination: PeerLocation) {
    Logger.fine(s"sendToken $token")
    val group = token.getGroup
    val proxyId = group match {
      case rgp: RemoteGroupProxy => rgp.remoteProxyId
      case _ => freshGroupProxyId()
    }
    val rmtProxy = new RemoteGroupMembersProxy(group, sendKill(destination, proxyId), proxyId)
    proxiedGroupMembers.put(proxyId, rmtProxy)

    group.add(rmtProxy)
    group.remove(token)

    destination.send(HostTokenCmd(executionId, new TokenReplacement(token, node, rmtProxy.thisProxyId, destination)))
  }

  def hostToken(origin: PeerLocation, movedToken: TokenReplacement) {
    val lookedUpProxyGroupMember = proxiedGroupMembers.get(movedToken.tokenProxyId)
    val newTokenGroup = lookedUpProxyGroupMember match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(this, movedToken.tokenProxyId, sendPublish(origin, movedToken.tokenProxyId), sendHalt(origin, movedToken.tokenProxyId), sendDiscorporate(origin, movedToken.tokenProxyId))
        proxiedGroups.put(movedToken.tokenProxyId, rgp)
        rgp
      }
      case gmp => gmp.parent
    }
    val newToken = movedToken.asToken(origin, node, newTokenGroup)
    if (lookedUpProxyGroupMember != null) {
      /* Discard unused RemoteGroupMenbersProxy */
      origin.send(HaltGroupMemberProxyCmd(executionId, movedToken.tokenProxyId))
    }
    Logger.fine(s"scheduling $newToken")
    runtime.schedule(newToken)
  }

  def sendPublish(destination: PeerLocation, proxyId: GroupProxyId)(token: Token, pv: Option[AnyRef]) {
    Logger.fine(s"sendPublish: publish by token $token")
    destination.send(PublishGroupCmd(executionId, proxyId, new PublishingTokenReplacement(token, node, proxyId, destination, pv)))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: GroupProxyId, publishingToken: PublishingTokenReplacement) {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publishingToken))
    val newTokenGroup = proxiedGroupMembers.get(publishingToken.tokenProxyId).parent
    val newToken = publishingToken.asPublishingToken(origin, node, newTokenGroup)
    Logger.fine(s"publishInGroup $newToken")
    runtime.schedule(newToken)
  }

  def sendHalt(destination: PeerLocation, groupMemberProxyId: GroupProxyId)() {
    destination.send(HaltGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def sendDiscorporate(destination: PeerLocation, groupMemberProxyId: GroupProxyId)() {
    destination.send(DiscorporateGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.halt() } })
      proxiedGroupMembers.remove(groupMemberProxyId)
    } else {
      Logger.fine(s"halt group member proxy on unknown group member proxy $groupMemberProxyId")
    }
  }

  def discorporateGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.halt() } })
      proxiedGroupMembers.remove(groupMemberProxyId)
    } else {
      Logger.fine(s"halt group member proxy on unknown group member proxy $groupMemberProxyId")
    }
  }

  def sendKill(destination: PeerLocation, proxyId: GroupProxyId)() {
    destination.send(KillGroupCmd(executionId, proxyId))
  }

  def killGroupProxy(proxyId: GroupProxyId) {
    val g = proxiedGroups.get(proxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.kill() } })
      proxiedGroups.remove(proxyId)
    } else {
      Logger.fine(s"kill group on unknown group $proxyId")
    }
  }
}
