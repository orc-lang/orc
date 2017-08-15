//
// GroupProxy.scala -- Scala classes RemoteGroupProxy and RemoteGroupMembersProxy, and trait GroupProxyManager
// Project PorcE
//
// Created by jthywiss on Dec 25, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.Schedulable
import orc.run.porce.runtime.{ CallClosureSchedulable, CallRecord, Counter, PorcEClosure, Terminatable, Terminator }

/** Proxy for a group the resides on a remote dOrc node.
  * RemoteGroupProxy is created locally when a token has been migrated from
  * another node. Notifications sent to this proxy from its members will be
  * passed to the execution.  When all local group members halt, the remote
  * group will be notified.  If the remote group is killed, this proxy will
  * pass the kill on to the local members.
  *
  * @author jthywiss
  */
class RemoteGroupProxy(
    val remoteProxyId: DOrcExecution#GroupProxyId,
    pubFunc: (AnyRef) => Unit,
    onHaltFunc: () => Unit,
    onDiscorporateFunc: () => Unit,
    onResurrectFunc: () => Unit) {

  val publicationContinuation: PorcEClosure = ???
  val counter = new CounterProxy()
  val terminator = new TerminatorProxy()

  class CounterProxy() extends Counter() {
    def remoteGroupProxy = RemoteGroupProxy.this

    override def onHalt() {
      //Logger.entering(getClass.getName, "onHalt")
      onHaltFunc()
    }

    /*override*/ def onDiscorporate() {
      //Logger.entering(getClass.getName, "onDiscorporate")
      onDiscorporateFunc()
    }

    override def onResurrect() = {
      //Logger.entering(getClass.getName, "onResurrect")
      onResurrectFunc()
    }

  }

  class TerminatorProxy() extends Terminator() {
    def remoteGroupProxy = RemoteGroupProxy.this

    override def kill(k: PorcEClosure): Boolean = {
      // First, swap in null as the children set.
      val cs = children.getAndSet(null)
      // Next, process cs if needed.
      // See description of ordering in addChild().
      if (cs != null) {
        // If we were the first to kill and it succeeded
        doKills(cs)
        true
      } else {
        // If it was already killed
        false
      }
    }

  }

  ///* PorcE doesn't do notification at the group level. Hrmpf. */
  //override def notifyOrc(event: OrcEvent) = {
  //  Logger.entering(getClass.getName, "notifyOrc", Seq(event))
  //  execution.notifyOrc(event)
  //  Logger.exiting(getClass.getName, "notifyOrc")
  //}

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
class RemoteGroupMembersProxy(val thisProxyId: DOrcExecution#GroupProxyId, val publicationContinuation: PorcEClosure, val enclosingCounter: Counter, val enclosingTerminator: Terminator, sendKillFunc: () => Unit) extends Terminatable {
  private var alive = true

  /** Remote group members all halted, so halt this. */
  def halt() = synchronized {
    //Logger.entering(getClass.getName, "halt")
    if (alive) {
      alive = false
      enclosingCounter.haltToken()
    }
  }

  /** Remote group members all halted, but there was discorporates, so discorporate this. */
  def discorporate() = synchronized {
    //Logger.entering(getClass.getName, "discorporate")
    if (alive) {
      alive = false
      enclosingCounter.discorporateToken()
    }
  }

  /** The group is killing its members; pass it on. */
  override def kill() = synchronized {
    //Logger.entering(getClass.getName, "kill")
    if (alive) {
      alive = false
      sendKillFunc()
      Logger.finer(s"RemoteGroupMembersProxy.kill: enclosingCounter=${enclosingCounter.get}")
      enclosingCounter.haltToken()
    }
  }

  ///* PorcE doesn't do notification at the group level. Hrmpf. */
  //override def notifyOrc(event: OrcEvent) = execution.notifyOrc(event)

}

/** A mix-in to manage proxied groups.
  *
  * @author jthywiss
  */
trait GroupProxyManager {
  self: DOrcExecution =>
  type GroupProxyId = Long

  protected val proxiedGroups = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupProxy]
  protected val proxiedGroupMembers = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupMembersProxy]

  def sendCall(callRecord: CallRecord, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation) {
    Logger.fine(s"sendCall $callRecord, $callTarget, $callArguments, $destination")
    val publicationContinuation = callRecord.p
    val enclosingCounter = callRecord.c
    val enclosingTerminator = callRecord.t

    val proxyId = enclosingTerminator match {
      case tp: RemoteGroupProxy#TerminatorProxy => tp.remoteGroupProxy.remoteProxyId
      case _ => freshGroupProxyId()
    }
    val rmtProxy = new RemoteGroupMembersProxy(proxyId, publicationContinuation, enclosingCounter, enclosingTerminator, () => sendKill(destination, proxyId)())
    proxiedGroupMembers.put(proxyId, rmtProxy)

    enclosingTerminator.addChild(rmtProxy)
    enclosingTerminator.removeChild(???)

    /* Counter is correct as is, since we're swapping this call with its proxy. */

    Tracer.traceCallSend(proxyId, self.runtime.here, destination)

    destination.sendInContext(self)(MigrateCallCmd(executionId, proxyId, callRecord, callTarget, callArguments))
  }

  def receiveCall(origin: PeerLocation, proxyId: DOrcExecution#GroupProxyId, movedCall: CallRecord, callTarget: AnyRef, callArguments: Array[AnyRef]) {
    val lookedUpProxyGroupMember = proxiedGroupMembers.get(proxyId)
    val (publicationContinuation: PorcEClosure, enclosingCounter: Counter, enclosingTerminator: Terminator) = lookedUpProxyGroupMember match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(proxyId, sendPublish(origin, proxyId)(_), () => sendHalt(origin, proxyId)(), () => sendDiscorporate(origin, proxyId)(), () => sendResurrect(origin, proxyId)())
        proxiedGroups.put(proxyId, rgp)
        (rgp.publicationContinuation, rgp.counter, rgp.terminator)
      }
      case gmp => (gmp.publicationContinuation, gmp.enclosingCounter, gmp.enclosingTerminator)
    }

    val callInvoker = new Schedulable { def run() = { self.invokeCallRecord(movedCall, callTarget, callArguments) } }
    enclosingCounter.newToken()
    enclosingTerminator.addChild(???)

    if (lookedUpProxyGroupMember != null) {
      /* We have a RemoteGroupMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */
      Tracer.traceHaltGroupMemberSend(proxyId, self.runtime.here, origin)
      origin.sendInContext(self)(HaltGroupMemberProxyCmd(executionId, proxyId))
    }

    Tracer.traceCallReceive(proxyId, origin, self.runtime.here)

    Logger.fine(s"scheduling $callInvoker")
    runtime.schedule(callInvoker)
  }

  def sendPublish(destination: PeerLocation, proxyId: GroupProxyId)(publishedValue: AnyRef) {
    Logger.fine(s"sendPublish: publish by proxyId $proxyId")
    Tracer.tracePublishSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(PublishGroupCmd(executionId, proxyId, publishedValue))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: GroupProxyId, publishedValue: AnyRef) {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publishedValue))
    val publicationContinuation = proxiedGroupMembers.get(groupMemberProxyId).publicationContinuation
    Tracer.tracePublishReceive(groupMemberProxyId, origin, self.runtime.here)
    runtime.schedule(CallClosureSchedulable(publicationContinuation, publishedValue))
  }

  def sendHalt(destination: PeerLocation, groupMemberProxyId: GroupProxyId)() {
    Tracer.traceHaltGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(HaltGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def sendDiscorporate(destination: PeerLocation, groupMemberProxyId: GroupProxyId)() {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(DiscorporateGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def sendResurrect(destination: PeerLocation, groupMemberProxyId: GroupProxyId)() {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(ResurrectGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.halt() } })
    } else {
      Logger.fine(f"Halt group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def discorporateGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.discorporate() } })
      proxiedGroupMembers.remove(groupMemberProxyId)
    } else {
      Logger.fine(f"Discorporate group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def resurrectGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      g.enclosingCounter.newToken()
    } else {
      Logger.fine(f"Resurrect group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def sendKill(destination: PeerLocation, proxyId: GroupProxyId)() {
    Tracer.traceKillGroupSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(KillGroupCmd(executionId, proxyId))
  }

  def killGroupProxy(proxyId: GroupProxyId) {
    val g = proxiedGroups.get(proxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.terminator.kill() } })
      proxiedGroups.remove(proxyId)
    } else {
      Logger.fine(f"Kill group on unknown group $proxyId%#x")
    }
  }
}
