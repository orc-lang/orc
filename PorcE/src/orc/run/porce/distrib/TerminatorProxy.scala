//
// TerminatorProxy.scala -- Scala classes RemoteTerminatorProxy and RemoteTerminatorMembersProxy, and trait TerminatorProxyManager
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

/** Proxy for a terminator the resides on a remote dOrc node.
  * RemoteTerminatorProxy is created locally when a token has been migrated from
  * another node. If the remote terminator is killed, this proxy will
  * pass the kill on to the local members.
  *
  * @author jthywiss
  */
class RemoteTerminatorProxy(val remoteProxyId: RemoteRef#RemoteRefId) extends Terminator() {

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
class RemoteTerminatorMembersProxy(
    val thisProxyId: RemoteRef#RemoteRefId,
    val enclosingCounter: Counter,
    val enclosingTerminator: Terminator,
    sendKillFunc: () => Unit)
  extends Terminatable {

  private var alive = true

  /** The parent terminator is killing its members; pass it on to the remote terminator proxy. */
  override def kill(): Unit = synchronized {
    //Logger.entering(getClass.getName, "kill")
    if (alive) {
      alive = false
      sendKillFunc()
      Logger.finer(s"RemoteTerminatorMembersProxy.kill: enclosingCounter=${enclosingCounter.get}")
      enclosingCounter.haltToken()
    }
  }

}

/** A mix-in to manage proxied terminators.
  *
  * @author jthywiss
  */
trait TerminatorProxyManager {
  self: DOrcExecution =>

  protected val proxiedTerminators = new java.util.concurrent.ConcurrentHashMap[RemoteRefId, RemoteTerminatorProxy]
  protected val proxiedTerminatorMembers = new java.util.concurrent.ConcurrentHashMap[RemoteRefId, RemoteTerminatorMembersProxy]

  def sendKill(destination: PeerLocation, proxyId: RemoteRefId)(): Unit = {
    Tracer.traceKillGroupSend(proxyId, self.runtime.here, destination)
    destination.sendInContext(self)(KillGroupCmd(executionId, proxyId))
  }

  def killGroupProxy(proxyId: RemoteRefId): Unit = {
    val g = proxiedTerminators.get(proxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run(): Unit = { g.kill() } })
      proxiedTerminators.remove(proxyId)
    } else {
      Logger.fine(f"Kill group on unknown group $proxyId%#x")
    }
  }
}
