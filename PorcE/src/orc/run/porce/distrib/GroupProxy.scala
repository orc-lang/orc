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
import orc.run.porce.runtime.CPSCallResponseHandler

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

  def kill(): Unit = {
    //Logger.entering(getClass.getName, "kill")
    /* All RemoteGroupProxy kills come from the remote side */
    terminator.kill()
  }

  class CounterProxy() extends Counter() {
    def remoteGroupProxy: RemoteGroupProxy = RemoteGroupProxy.this

    override def onHalt(): Unit = {
      //Logger.entering(getClass.getName, "onHalt")
      onHaltFunc()
    }

    /*override*/ def onDiscorporate(): Unit = {
      //Logger.entering(getClass.getName, "onDiscorporate")
      onDiscorporateFunc()
    }

    override def onResurrect(): Unit = {
      //Logger.entering(getClass.getName, "onResurrect")
      onResurrectFunc()
    }

  }

  class TerminatorProxy() extends Terminator() {
    def remoteGroupProxy: RemoteGroupProxy = RemoteGroupProxy.this

    override def kill(k: PorcEClosure): Boolean = {
      //Logger.entering(getClass.getName, "kill")
      /* All RemoteGroupProxy kills come from the remote side */
      super.kill(k)
    }

  }

  ///* PorcE doesn't do notification at the group level. Hrmpf. */
  //override def notifyOrc(event: OrcEvent): Unit {
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
  private var discorporated = false

  /** Remote group members all halted, so notify parent that we've halted. */
  def notifyParentOfHalt(): Unit = synchronized {
    //Logger.entering(getClass.getName, "halt")
    if (alive) {
      alive = false
      enclosingCounter.haltToken()
    }
  }

  /** Remote group members all halted, but there was discorporated members, so so notify parent that we've discorporated. */
  def notifyParentOfDiscorporate(): Unit = synchronized {
    //Logger.entering(getClass.getName, "discorporate")
    if (!discorporated && alive) {
      discorporated = true
      enclosingCounter.discorporateToken()
    } else {
      throw new IllegalStateException(s"RemoteGroupMembersProxy.notifyParentOfDiscorporate when alive=$alive, discorporated=$discorporated")
    }
  }

  /** Remote group had only discorporated members, but now has a new member, so notify parent that we've resurrected. */
  def notifyParentOfResurrect(): Unit = synchronized {
    //Logger.entering(getClass.getName, "resurrect")
    if (discorporated && alive) {
      discorporated = false
      enclosingCounter.newToken()
    } else {
      throw new IllegalStateException(s"RemoteGroupMembersProxy.notifyParentOfResurrect when alive=$alive, discorporated=$discorporated")
    }
  }

  /** The parent group is killing its members; pass it on to the remote group proxy. */
  override def kill(): Unit = synchronized {
    //Logger.entering(getClass.getName, "kill")
    if (alive) {
      alive = false
      sendKillFunc()
      Logger.finer(s"RemoteGroupMembersProxy.kill: enclosingCounter=${enclosingCounter.get}")
      enclosingCounter.haltToken()
    }
  }

  ///* PorcE doesn't do notification at the group level. Hrmpf. */
  //override def notifyOrc(event: OrcEvent): Unit = execution.notifyOrc(event)

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

  def sendCall(callHandler: CPSCallResponseHandler, callTarget: AnyRef, callArguments: Array[AnyRef], destination: PeerLocation): Unit = {
    Logger.fine(s"sendCall $callHandler, $callTarget, $callArguments, $destination")
    val publicationContinuation = callHandler.p
    val enclosingCounter = callHandler.c
    val enclosingTerminator = callHandler.t

    val proxyId = enclosingTerminator match {
      case tp: RemoteGroupProxy#TerminatorProxy => tp.remoteGroupProxy.remoteProxyId
      case _ => freshGroupProxyId()
    }
    val rmtProxy = new RemoteGroupMembersProxy(proxyId, publicationContinuation, enclosingCounter, enclosingTerminator, () => sendKill(destination, proxyId)())
    proxiedGroupMembers.put(proxyId, rmtProxy)

    enclosingTerminator.addChild(rmtProxy)
    enclosingTerminator.removeChild(callHandler)

    /* Counter is correct as is, since we're swapping this call with its proxy. */

    Tracer.traceCallSend(proxyId, self.runtime.here, destination)

    destination.sendInContext(self)(MigrateCallCmd(executionId, proxyId, callHandler.callRecord, callTarget, callArguments))
  }

  def receiveCall(origin: PeerLocation, proxyId: DOrcExecution#GroupProxyId, movedCall: CallRecord, callTarget: AnyRef, callArguments: Array[AnyRef]): Unit = {
    val lookedUpProxyGroupMember = proxiedGroupMembers.get(proxyId)
    val (proxyPublicationContinuation: PorcEClosure, proxyCounter: Counter, proxyTerminator: Terminator) = lookedUpProxyGroupMember match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(proxyId, sendPublish(origin, proxyId)(_), () => sendHalt(origin, proxyId)(), () => sendDiscorporate(origin, proxyId)(), () => sendResurrect(origin, proxyId)())
        proxiedGroups.put(proxyId, rgp)
        (rgp.publicationContinuation, rgp.counter, rgp.terminator)
      }
      case gmp => (gmp.publicationContinuation, gmp.enclosingCounter, gmp.enclosingTerminator)
    }

    val callRecord = new CallRecord(proxyPublicationContinuation, proxyCounter, proxyTerminator, movedCall.callSitePosition, movedCall.callSiteId)
    val callInvoker = new Schedulable { def run(): Unit = { self.invokeCallRecord(callRecord, callTarget, callArguments) } }
    proxyCounter.newToken()
    proxyTerminator.addChild(???)

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

  def sendPublish(destination: PeerLocation, proxyId: GroupProxyId)(publishedValue: AnyRef): Unit = {
    Logger.fine(s"sendPublish: publish by proxyId $proxyId")
    Tracer.tracePublishSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(PublishGroupCmd(executionId, proxyId, publishedValue))
  }

  def publishInGroup(origin: PeerLocation, groupMemberProxyId: GroupProxyId, publishedValue: AnyRef): Unit = {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publishedValue))
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      Tracer.tracePublishReceive(groupMemberProxyId, origin, self.runtime.here)
      runtime.schedule(CallClosureSchedulable(g.publicationContinuation, publishedValue))
    } else {
      throw new AssertionError(f"Publish by unknown group member proxy $groupMemberProxyId%#x, value=$publishedValue")
    }
  }

  def sendHalt(destination: PeerLocation, groupMemberProxyId: GroupProxyId)(): Unit = {
    Tracer.traceHaltGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(HaltGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def sendDiscorporate(destination: PeerLocation, groupMemberProxyId: GroupProxyId)(): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(DiscorporateGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def sendResurrect(destination: PeerLocation, groupMemberProxyId: GroupProxyId)(): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, self.runtime.here, destination)
    destination.sendInContext(self)(ResurrectGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: GroupProxyId): Unit = {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfHalt() } })
    } else {
      Logger.fine(f"Halt group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def discorporateGroupMemberProxy(groupMemberProxyId: GroupProxyId): Unit = {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfDiscorporate() } })
      proxiedGroupMembers.remove(groupMemberProxyId)
    } else {
      Logger.fine(f"Discorporate group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def resurrectGroupMemberProxy(groupMemberProxyId: GroupProxyId): Unit = {
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfResurrect() } })
    } else {
      Logger.fine(f"Resurrect group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def sendKill(destination: PeerLocation, proxyId: GroupProxyId)(): Unit = {
    Tracer.traceKillGroupSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(KillGroupCmd(executionId, proxyId))
  }

  def killGroupProxy(proxyId: GroupProxyId): Unit = {
    val g = proxiedGroups.get(proxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run(): Unit = { g.kill() } })
      proxiedGroups.remove(proxyId)
    } else {
      Logger.fine(f"Kill group on unknown group $proxyId%#x")
    }
  }
}
