//
// TerminatorProxy.scala -- Scala trait TerminatorProxyManager, and classes RemoteTerminatorProxy and RemoteTerminatorMembersProxy
// Project PorcE
//
// Created by jthywiss on Aug 15, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.Schedulable
import orc.run.porce.runtime.{ CallClosureSchedulable, Counter, PorcEClosure, Terminatable, Terminator }

/** A DOrcExecution mix-in to create and communicate among proxied terminators.
  *
  * @see TerminatorProxyManager#RemoteTerminatorProxy
  * @see TerminatorProxyManager#RemoteTerminatorMembersProxy
  * @author jthywiss
  */
trait TerminatorProxyManager {
  execution: DOrcExecution =>

  type TerminatorProxyId = RemoteRefId

  /** Proxy for a terminator the resides on a remote dOrc node.
    * RemoteTerminatorProxy is created locally when a token has been migrated from
    * another node. If the remote terminator is killed, this proxy will
    * pass the kill on to the local members.
    *
    * @author jthywiss
    */
  class RemoteTerminatorProxy private[TerminatorProxyManager] (
      val remoteProxyId: TerminatorProxyManager#TerminatorProxyId,
      onKill: (Counter, PorcEClosure) => Unit)
    extends Terminator() {

    override def toString: String = f"${getClass.getName}(remoteProxyId=$remoteProxyId%#x)"

    override def kill(c: Counter, k: PorcEClosure): Boolean = {
      //Logger.info(s"kill on $this")
      require(c != null)
      require(k != null)
      //Logger.entering(getClass.getName, "kill")
      // Token: Pass a token on c to onKill
      onKill(c, k)
      false
    }

    override def kill(): Unit = {
      super.kill(null, null)
    }
  }

  /** Proxy for one or more remote members of a terminator.
    * RemoteTerminatorMembersProxy is created when a token is migrated to another node.
    * As the migrated token continues to execute, this proxy may come to represent
    * multiple tokens, and/or sub-terminators. If this proxy is killed, it will
    * pass the kill on to its remote members.
    *
    * @author jthywiss
    */
  final class RemoteTerminatorMembersProxy private[TerminatorProxyManager] (
      val thisProxyId: TerminatorProxyManager#TerminatorProxyId,
      val enclosingTerminator: Terminator,
      sendKillFunc: (TerminatorProxyManager#TerminatorProxyId) => Unit)
    extends Terminatable {

    // FIXME: PERFORMANCE: Make this an atomic boolean or whatever state is needed. It may not save much, but it cannot be worse than a lock (since the atomic will be a single lock instruction).
    private var alive = true

    override def toString: String = f"${getClass.getName}(thisProxyId=$thisProxyId%#x, enclosingTerminator=$enclosingTerminator)"

    /** The parent terminator is killing its members; pass it on to the remote terminator proxy. */
    override def kill(): Unit = synchronized {
      //Logger.entering(getClass.getName, "kill")
      if (alive) {
        //Logger.finer(s"RemoteTerminatorMembersProxy.kill")
        alive = false
        sendKillFunc(thisProxyId)
      }
    }
  }

  protected val proxiedTerminators = new java.util.concurrent.ConcurrentHashMap[TerminatorProxyId, RemoteTerminatorProxy]
  protected val proxiedTerminatorMembers = new java.util.concurrent.ConcurrentHashMap[TerminatorProxyId, RemoteTerminatorMembersProxy]
  protected val proxiedTerminatorMembersByTerminator = new java.util.concurrent.ConcurrentHashMap[Terminator, TerminatorProxyId]

  def makeProxyWithinTerminator(enclosingTerminator: Terminator, sendKillFunc: (TerminatorProxyId) => Unit) = {
    val terminatorProxyId = (enclosingTerminator: @unchecked) match {
      case tp: RemoteTerminatorProxy => tp.remoteProxyId
      case _ => proxiedTerminatorMembersByTerminator.computeIfAbsent(enclosingTerminator, (_) => freshRemoteRefId())
    }

    val rmtTerminatorMbrProxy = proxiedTerminatorMembers.computeIfAbsent(terminatorProxyId, (_) => 
      new RemoteTerminatorMembersProxy(terminatorProxyId, enclosingTerminator, { (id) =>
        proxiedTerminatorMembers.remove(terminatorProxyId)
        proxiedTerminatorMembersByTerminator.remove(enclosingTerminator)
        sendKillFunc(id)
      }))

    /* Add rmtTerminatorMbrProxy in enclosingTerminator to get killed notifications */
    enclosingTerminator.addChild(rmtTerminatorMbrProxy)

    terminatorProxyId
  }

  def makeProxyTerminatorFor(terminatorProxyId: TerminatorProxyId, origin: PeerLocation): Terminator = {
    val lookedUpProxyTerminatorMember = proxiedTerminatorMembers.get(terminatorProxyId)
    val proxyTerminator = lookedUpProxyTerminatorMember match {
      case null => { /* Not a terminator we created at this node */
        //val rtp = new RemoteTerminatorProxy(terminatorProxyId)
        //proxiedTerminators.put(terminatorProxyId, rtp)
        proxiedTerminators.computeIfAbsent(terminatorProxyId, (_) => {
          new RemoteTerminatorProxy(terminatorProxyId, { (counter, closure) => 
            proxiedTerminators.remove(terminatorProxyId)
            sendKilling(origin, terminatorProxyId)(counter, closure)
          })
        })
      }
      case rtmp => rtmp.enclosingTerminator
    }

    proxyTerminator
  }

  def sendKilled(destination: PeerLocation, proxyId: TerminatorProxyId)(): Unit = {
    Tracer.traceKillGroupSend(proxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(KilledGroupCmd(execution.executionId, proxyId))
  }

  def sendKilling(destination: PeerLocation, proxyId: TerminatorProxyId)(counter: Counter, continuation: PorcEClosure): Unit = {
    Tracer.traceKillGroupSend(proxyId, execution.runtime.here, destination)
    val dc = getDistributedCounterForCounter(counter)
    // Token: Convert token into credits
    val credits = dc.convertToken()
    // Credit: Pass with message
    destination.sendInContext(execution)(KillingGroupCmd(execution.executionId, proxyId, new KillingMemento(dc.id, credits, continuation)))
  }

  def killedGroupProxy(proxyId: TerminatorProxyId): Unit = {
    // TODO: Does this need to be atomic?
    val g = proxiedTerminators.get(proxyId)
    if (g != null) {
      Logger.fine(s"Scheduling $g.kill()")
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.kill() } })
      proxiedTerminators.remove(proxyId)
    } else {
      Logger.fine(f"Kill group on unknown (or already killed) group $proxyId%#x")
    }
  }

  def killingGroupProxy(origin: PeerLocation, proxyId: TerminatorProxyId, killing: KillingMemento): Unit = {
    val m = proxiedTerminatorMembers.get(proxyId)
    val dc = getDistributedCounterForId(killing.counterId)
    // Token: Pass the token on the message to the local proxy
    dc.activate(killing.credit)
    if (m != null) {
      val g = m.enclosingTerminator
      Logger.fine(s"Scheduling $g.kill($dc, ${killing.continuation})...")
      execution.runtime.schedule(new Schedulable {
        def run(): Unit = {
          if (g.kill(dc.counter, killing.continuation)) {
            // No reason to schedule here.
            CallClosureSchedulable(killing.continuation).run()
          }
        }
      })
      proxiedTerminatorMembers.remove(proxyId)
      proxiedTerminatorMembersByTerminator.remove(g)
    } else {
      // The group will be "unknown" if it has already been killed so we need to halt the counter.
      dc.counter.haltToken()
    }
  }
}
