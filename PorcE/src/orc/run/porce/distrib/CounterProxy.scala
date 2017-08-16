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
    * @author jthywiss
    */
  class RemoteCounterProxy private[CounterProxyManager] (
      val remoteProxyId: CounterProxyManager#CounterProxyId,
      onHaltFunc: () => Unit,
      onDiscorporateFunc: () => Unit,
      onResurrectFunc: () => Unit)
    extends Counter() {
  
    override def onHalt(): Unit = {
      //Logger.entering(getClass.getName, "onHalt")
      if (isDiscorporated) onDiscorporateFunc()
      onHaltFunc()
    }
  
    override def onResurrect(): Unit = {
      //Logger.entering(getClass.getName, "onResurrect")
      onResurrectFunc()
    }
  
  }
  
  /** Proxy for one or more remote members of a counter (tokens and counters).
    * RemoteCounterMembersProxy is created when a token is migrated to another node.
    * As the migrated token continues to execute, this proxy may come to represent
    * multiple tokens, and/or sub-counters.  When all remote members halt, this
    * proxy will remove itself from the parent.
    *
    * @author jthywiss
    */
  class RemoteCounterMembersProxy private[CounterProxyManager] (
      val thisProxyId: CounterProxyManager#CounterProxyId,
      val enclosingCounter: Counter) {
    private var alive = true
    private var discorporated = false
  
    /** Remote counter members all halted, so notify parent that we've halted. */
    def notifyParentOfHalt(): Unit = synchronized {
      //Logger.entering(getClass.getName, "halt")
      if (alive) {
        alive = false
        enclosingCounter.haltToken()
      }
    }
  
    /** Remote counter members all halted, but there was discorporated members, so so notify parent that we've discorporated. */
    def notifyParentOfDiscorporate(): Unit = synchronized {
      //Logger.entering(getClass.getName, "discorporate")
      if (!discorporated && alive) {
        discorporated = true
        enclosingCounter.discorporateToken()
      } else {
        throw new IllegalStateException(s"RemoteCounterMembersProxy.notifyParentOfDiscorporate when alive=$alive, discorporated=$discorporated")
      }
    }
  
    /** Remote counter had only discorporated members, but now has a new member, so notify parent that we've resurrected. */
    def notifyParentOfResurrect(): Unit = synchronized {
      //Logger.entering(getClass.getName, "resurrect")
      if (discorporated && alive) {
        discorporated = false
        enclosingCounter.newToken()
      } else {
        throw new IllegalStateException(s"RemoteCounterMembersProxy.notifyParentOfResurrect when alive=$alive, discorporated=$discorporated")
      }
    }
  
  }

  protected val proxiedCounters = new java.util.concurrent.ConcurrentHashMap[CounterProxyId, RemoteCounterProxy]
  protected val proxiedCounterMembers = new java.util.concurrent.ConcurrentHashMap[CounterProxyId, RemoteCounterMembersProxy]

  def makeProxyWithinCounter(enclosingCounter: Counter): CounterProxyId = {
    val counterProxyId = enclosingCounter match {
      case cp: RemoteCounterProxy => cp.remoteProxyId
      case _ => freshRemoteRefId()
    }
    val rmtCounterMbrProxy = new RemoteCounterMembersProxy(counterProxyId, enclosingCounter)
    proxiedCounterMembers.put(counterProxyId, rmtCounterMbrProxy)
    
    /* enclosingCounter value is correct as is, since we're swapping this "token" with its proxy. */

    counterProxyId    
  }

  def makeProxyCounterFor(counterProxyId: CounterProxyId, origin: PeerLocation): Counter = {
    val lookedUpProxyCounterMember = proxiedCounterMembers.get(counterProxyId)
    val proxyCounter = lookedUpProxyCounterMember match {
      case null => { /* Not a counter we've seen before */
        val rcp = new RemoteCounterProxy(counterProxyId, () => execution.sendHalt(origin, counterProxyId)(), () => sendDiscorporate(origin, counterProxyId)(), () => sendResurrect(origin, counterProxyId)())
        proxiedCounters.put(counterProxyId, rcp)
        rcp
      }
      case rcmp => rcmp.enclosingCounter
    }

    if (lookedUpProxyCounterMember != null) {
      /* There is a RemoteCounterMembersProxy already for this proxyId,
       * so discard the superfluous one created at the sender. */
      Tracer.traceHaltGroupMemberSend(counterProxyId, execution.runtime.here, origin)
      origin.sendInContext(execution)(HaltGroupMemberProxyCmd(execution.executionId, counterProxyId))
    }

    proxyCounter
  }

  def sendHalt(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(): Unit = {
    Tracer.traceHaltGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(HaltGroupMemberProxyCmd(execution.executionId, groupMemberProxyId))
  }

  def sendDiscorporate(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(DiscorporateGroupMemberProxyCmd(execution.executionId, groupMemberProxyId))
  }

  def sendResurrect(destination: PeerLocation, groupMemberProxyId: CounterProxyId)(): Unit = {
    Tracer.traceDiscorporateGroupMemberSend(groupMemberProxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(ResurrectGroupMemberProxyCmd(execution.executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: CounterProxyId): Unit = {
    val g = proxiedCounterMembers.get(groupMemberProxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfHalt() } })
    } else {
      Logger.fine(f"Halt group member proxy on unknown group member proxy $groupMemberProxyId%#x")
    }
  }

  def discorporateGroupMemberProxy(groupMemberProxyId: CounterProxyId): Unit = {
    val g = proxiedCounterMembers.get(groupMemberProxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.notifyParentOfDiscorporate() } })
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
