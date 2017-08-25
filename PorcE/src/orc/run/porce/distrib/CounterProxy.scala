//
// CounterProxy.scala -- Scala trait CounterProxyManager, and classes RemoteCounterProxy and RemoteCounterMembersProxy
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

import java.util.concurrent.atomic.AtomicInteger

import orc.Schedulable
import orc.run.porce.runtime.Counter

/** A DOrcExecution mix-in to create and communicate among proxied counters.
  *
  * @see CounterProxyManager#RemoteCounterProxy
  * @see CounterProxyManager#RemoteCounterMembersProxy
  * @author jthywiss
  */
trait CounterProxyManager {
  execution: DOrcExecution =>

  type CounterProxyId = RemoteRefId

  /** Proxy for a counter the resides on a remote dOrc node.
    * RemoteCounterProxy is created locally when a token has been migrated from
    * another node. When all local counter members halt, the remote
    * counter will be notified.
    *
    * This counter can maintain multiple parent tokens.
    *
    * @author jthywiss
    */
  class RemoteCounterProxy private[CounterProxyManager] (
      val remoteProxyId: CounterProxyManager#CounterProxyId,
      onHaltFunc: (Int) => Unit,
      onDiscorporateFunc: (Int) => Unit,
      onResurrectFunc: () => Unit)
    extends Counter(0) {
    // Proxy counters start "floating" in a non-halted, but 0 count situation.
    val parentTokens = new AtomicInteger(0)

    override def onHalt(): Unit = {
      //Logger.entering(getClass.getName, "onHalt")
      val n = parentTokens.getAndSet(0)
      logChange(s"onHalt from $n (at ${get()})")
      assert(n > 0)
      if (isDiscorporated) {
        onDiscorporateFunc(n)
      } else {
        onHaltFunc(n)
      }
    }

    override def onResurrect(): Unit = {
      //Logger.entering(getClass.getName, "onResurrect")
      onResurrectFunc()
    }

    override def toString: String = f"${getClass.getName}(remoteProxyId=$remoteProxyId%#x)"

    /** Take a parent token and add it to this proxy.
     *
     *  The parent token will be handled through it's halting by the proxy. The
     *  caller to this method gets a token on this proxy.
     *
     *  This call will never require a newToken from the parent. Instead, the
     *  caller must provide that token.
     */
    final def takeParentToken(): Unit = {
      val n = incrementAndGet()
      logChange(s"takeParentToken to $n")
      parentTokens.getAndIncrement()
    }
  }

  /** Proxy for one or more remote members of a counter (tokens and counters).
    * RemoteCounterMembersProxy is created when a token is migrated to another node.
    * As the migrated token continues to execute, this proxy may come to represent
    * multiple tokens, and/or sub-counters.  When all remote members halt, this
    * proxy will remove itself from the parent.
    * 
    * This proxy can be reused and holds no state about whether it is no longer 
    * needed.
    *
    * @author jthywiss, amp
    */
  final class RemoteCounterMembersProxy private[CounterProxyManager] (
      val thisProxyId: CounterProxyManager#CounterProxyId,
      val enclosingCounter: Counter) {
    // TODO: SAFETY: Are all calls into methods on this object guaranteed to be from the same receiver thread.
    //private var parentTokens = 0
    // TODO: MEMORYLEAK: The lack of any local information on if this members proxy is done will make removing proxies from the maps very hard.

    /** Remote counter members all halted, so notify parent that we've halted. */
    def notifyParentOfHalt(n: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"halt $n")
      //assert(parentTokens >= n)
      //parentTokens -= n
      (0 until n).foreach { (_) =>
        enclosingCounter.haltToken()
      }
    }

    /** Remote counter members all halted, but there was discorporated members, so so notify parent that we've discorporated. */
    def notifyParentOfDiscorporate(n: Int): Unit = synchronized {
      Logger.entering(getClass.getName, s"discorporate $n")
      //assert(parentTokens >= n)
      //parentTokens -= n
      (0 until n).foreach { (_) =>
        enclosingCounter.discorporateToken()
      }
    }
    
    def takeParentToken() = {
      Logger.entering(getClass.getName, s"takeParentToken")
      //parentTokens += 1      
    }

    /** Remote counter had only discorporated members, but now has a new member, so notify parent that we've resurrected. */
    def notifyParentOfResurrect(): Unit = synchronized {
      //Logger.entering(getClass.getName, "resurrect")
      enclosingCounter.newToken()
    }

  }

  protected val proxiedCountersById = new java.util.concurrent.ConcurrentHashMap[Counter, CounterProxyId]
  protected val proxiedCounters = new java.util.concurrent.ConcurrentHashMap[CounterProxyId, RemoteCounterProxy]
  protected val proxiedCounterMembers = new java.util.concurrent.ConcurrentHashMap[CounterProxyId, RemoteCounterMembersProxy]

  def makeProxyWithinCounter(enclosingCounter: Counter): CounterProxyId = {
    val counterProxyId = enclosingCounter match {
      case cp: RemoteCounterProxy => cp.remoteProxyId
      case _ => proxiedCountersById.computeIfAbsent(enclosingCounter, (_) => freshRemoteRefId())
    }
    proxiedCounterMembers.computeIfAbsent(counterProxyId, (_) => {
      val rmtCounterMbrProxy = new RemoteCounterMembersProxy(counterProxyId, enclosingCounter)
      Logger.finer(f"Created proxy for $enclosingCounter with id $counterProxyId")
      rmtCounterMbrProxy
    })
    
    /* Token: enclosingCounter value is correct as is, since we're swapping this "token" with its proxy. */

    counterProxyId
  }

  def makeProxyCounterFor(counterProxyId: CounterProxyId, origin: PeerLocation): RemoteCounterProxy = {
    def newProxyCounter =
      proxiedCounters.computeIfAbsent(counterProxyId, (_) =>
        new RemoteCounterProxy(counterProxyId,
          (n) => execution.sendHalt(origin, counterProxyId)(n),
          (n) => sendDiscorporate(origin, counterProxyId)(n),
          () => sendResurrect(origin, counterProxyId)()))
    newProxyCounter

    // FIXME: Reenable this optimization. It's probably important. However it will require figuring out both how to
    // make sure things are safe (in terms of message orderings) and how to handle the case where a proxy is passed
    // here when we actually have the counter. This may require the sender knowing if this will happen.
    /*
    val lookedUpProxyCounterMember = proxiedCounterMembers.get(counterProxyId)
    val proxyCounter = lookedUpProxyCounterMember match {
      case null => { /* Not a counter we've seen before */
      }
      case rcmp => rcmp.enclosingCounter
    }

    if (lookedUpProxyCounterMember != null) {
      /* There is a RemoteCounterMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */

      // FIXME: We should be able to remove this once proxies can track multiple tokens from their parent.
      // If we are moving from a remote proxy to a local original counter we will probably need to make a new token here.
      // The problem is what happens to the count on he remote proxy. We would need to somehow halt it. So we might
      // have to send this message anyway.
      Tracer.traceHaltGroupMemberSend(counterProxyId, execution.runtime.here, origin)
      origin.sendInContext(execution)(HaltGroupMemberProxyCmd(execution.executionId, counterProxyId, 1))
    }

    proxyCounter
    */
  }

  def sendHalt(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(n: Int): Unit = {
    Tracer.traceHaltGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(HaltGroupMemberProxyCmd(execution.executionId, groupMemberProxyId, n))
  }

  def sendDiscorporate(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(n: Int): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(DiscorporateGroupMemberProxyCmd(execution.executionId, groupMemberProxyId, n))
  }

  def sendResurrect(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(ResurrectGroupMemberProxyCmd(execution.executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: CounterProxyId, n: Int): Unit = {
    val g = proxiedCounterMembers.get(groupMemberProxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfHalt(n) } })
    } else {
      Logger.fine(f"Halt group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def discorporateGroupMemberProxy(groupMemberProxyId: CounterProxyId, n: Int): Unit = {
    val g = proxiedCounterMembers.get(groupMemberProxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfDiscorporate(n) } })
    } else {
      Logger.fine(f"Discorporate group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def resurrectGroupMemberProxy(groupMemberProxyId: CounterProxyId): Unit = {
    val g = proxiedCounterMembers.get(groupMemberProxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfResurrect() } })
    } else {
      Logger.fine(f"Resurrect group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

}
