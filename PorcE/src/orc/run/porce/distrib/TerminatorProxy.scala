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
import orc.run.porce.runtime.{ Counter, PorcEClosure, Terminatable, Terminator }

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
      val remoteProxyId: TerminatorProxyManager#TerminatorProxyId)
    extends Terminator() {
  
    override def kill(k: PorcEClosure): Boolean = {
      //Logger.entering(getClass.getName, "kill")
      /* All RemoteTerminatorProxy kills come from the remote side */
      super.kill(k)
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
  class RemoteTerminatorMembersProxy private[TerminatorProxyManager] (
      val thisProxyId: TerminatorProxyManager#TerminatorProxyId,
      val enclosingTerminator: Terminator,
      sendKillFunc: (TerminatorProxyManager#TerminatorProxyId) => Unit)
    extends Terminatable {
  
    private var alive = true
  
    /** The parent terminator is killing its members; pass it on to the remote terminator proxy. */
    override def kill(): Unit = synchronized {
      //Logger.entering(getClass.getName, "kill")
      if (alive) {
        Logger.finer(s"RemoteTerminatorMembersProxy.kill")
        alive = false
        sendKillFunc(thisProxyId)
      }
    }
  
  }

  protected val proxiedTerminators = new java.util.concurrent.ConcurrentHashMap[TerminatorProxyId, RemoteTerminatorProxy]
  protected val proxiedTerminatorMembers = new java.util.concurrent.ConcurrentHashMap[TerminatorProxyId, RemoteTerminatorMembersProxy]

  def makeProxyWithinTerminator(enclosingTerminator: Terminator, sendKillFunc: (TerminatorProxyId) => Unit) = {
    val terminatorProxyId = enclosingTerminator match {
      case tp: RemoteTerminatorProxy => tp.remoteProxyId
      case _ => freshRemoteRefId()
    }

    val rmtTerminatorMbrProxy = new RemoteTerminatorMembersProxy(terminatorProxyId, enclosingTerminator, sendKillFunc)
    proxiedTerminatorMembers.put(terminatorProxyId, rmtTerminatorMbrProxy)

    /* Add rmtTerminatorMbrProxy in enclosingTerminator to get killed notifications */
    enclosingTerminator.addChild(rmtTerminatorMbrProxy)

    terminatorProxyId
  }

  def makeProxyTerminatorFor(terminatorProxyId: TerminatorProxyId): Terminator = {
    val lookedUpProxyTerminatorMember = proxiedTerminatorMembers.get(terminatorProxyId)
    val proxyTerminator = lookedUpProxyTerminatorMember match {
      case null => { /* Not a terminator we created at this node */
        //val rtp = new RemoteTerminatorProxy(terminatorProxyId)
        //proxiedTerminators.put(terminatorProxyId, rtp)
        proxiedTerminators.computeIfAbsent(terminatorProxyId, (_) => new RemoteTerminatorProxy(terminatorProxyId))
      }
      case rtmp => rtmp.enclosingTerminator
    }

    proxyTerminator
  }

  def sendKill(destination: PeerLocation, proxyId: TerminatorProxyId)(): Unit = {
    Tracer.traceKillGroupSend(proxyId, execution.runtime.here, destination)
    destination.sendInContext(execution)(KillGroupCmd(execution.executionId, proxyId))
  }

  def killGroupProxy(proxyId: TerminatorProxyId): Unit = {
    val g = proxiedTerminators.get(proxyId)
    if (g != null) {
      execution.runtime.schedule(new Schedulable { def run(): Unit = { g.kill() } })
      proxiedTerminators.remove(proxyId)
    } else {
      Logger.fine(f"Kill group on unknown group $proxyId%#x")
    }
  }
}
