//
// DOrcRuntime.scala -- Scala class LeaderRuntime
// Project OrcScala
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import java.net.InetSocketAddress
import scala.ref.WeakReference
import scala.util.control.NonFatal
import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions }
import orc.ast.oil.nameless.Expression
import orc.ast.oil.xml.OrcXML
import orc.run.StandardOrcRuntime
import orc.run.core.{ Execution, Token }
import orc.run.extensions.SupportForDOrc
import orc.util.{ ConnectionInitiator, SocketObjectConnection }
import java.util.concurrent.atomic.AtomicLong

/** Orc runtime engine leading a dOrc cluster.
  *
  * @author jthywiss
  */
class LeaderRuntime() extends StandardOrcRuntime("dOrc leader") with
  SupportForDOrc {

  type ExecutionId = String
  type GroupProxyId = Long

  val followerConnections = scala.collection.mutable.Set.empty[SocketObjectConnection[(GroupProxyId, OrcEvent), OrcCmd]]
  val proxiedGroupMembers = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupMembersProxy]

  override def run(programAst: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    val programOil = OrcXML.astToXml(programAst).toString()
    val thisRun = java.util.UUID.randomUUID().toString
    val followers = Set(new InetSocketAddress("localhost", 36721), new InetSocketAddress("localhost", 36722))
    followers foreach { f => followerConnections.add(ConnectionInitiator[(GroupProxyId, OrcEvent), OrcCmd](f)) }
    followerConnections foreach { c => (new ReceiveThread(c, k)).start() }

    followerConnections foreach { _.send(LoadProgramCmd(thisRun, programOil, options)) }

    val root = new Execution(programAst, options, { e => handleExecutionEvent(thisRun, e); k(e) }, this)
    installHandlers(root)
    roots.put(new WeakReference(root), ())

    sendToken(new Token(programAst, root), followerConnections.head, thisRun, programAst)
    //followerConnections foreach { c => sendToken(new Token(programAst, root), c, thisRun, programAst) }
    //followerConnections foreach { c => sendToken(new Token(programAst, root), c, thisRun, programAst) }
    //followerConnections foreach { c => sendToken(new Token(programAst, root), c, thisRun, programAst) }

    Logger.exiting(getClass.getName, "run")
  }

  protected def sendToken(token: Token, destination: SocketObjectConnection[(GroupProxyId, OrcEvent), OrcCmd], executionId: ExecutionId, programAst: Expression) {
    val group = token.getGroup
    val proxyId = LeaderRuntime.nextGroupProxyId.getAndIncrement()
    val rmtProxy = new RemoteGroupMembersProxy(group, { () => destination.send(KillGroupCmd(proxyId)) }, proxyId)
    proxiedGroupMembers.put(proxyId, rmtProxy)

    group.add(rmtProxy)
    group.remove(token)

    destination.send(HostTokenCmd(executionId, new TokenReplacement(token, programAst, rmtProxy)))
  }

  protected def handleExecutionEvent(executionId: ExecutionId, event: OrcEvent) {
    Logger.fine(s"Execution got $event")
    event match {
      case HaltedOrKilledEvent => followerConnections foreach { _.send(UnloadProgramCmd(executionId)) }
      case _ => { /* ignore */ }
    }
  }

  protected class ReceiveThread(followerConnection: SocketObjectConnection[(GroupProxyId, OrcEvent), OrcCmd], k: OrcEvent => Unit)
      extends Thread(s"dOrc leader receiver for ${followerConnection.socket}") {
    override def run() {
      try {
        Logger.info(s"Reading events from ${followerConnection.socket}")
        while (true) {
          Logger.finest(s"Posting read on ${followerConnection.socket}")
          val msg = followerConnection.receive()
          Logger.finest(s"Read ${msg}")
          msg match {
          case (LeaderRuntime.NO_GROUP, event) => LeaderRuntime.this synchronized {
            k(event)
          }
          case (id, HaltedOrKilledEvent) => proxiedGroupMembers.get(id).kill()
          case (id, event) => proxiedGroupMembers.get(id).notifyOrc(event)
          }
        }
      } catch {
        case _: java.io.EOFException => () /* Handle closed socket by falling through */
      } finally {
        try {
          followerConnection.close()
        } catch {
        case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore close failures at this point */
        }
        Logger.info(s"Stopped reading events from ${followerConnection.socket}")
        followerConnections synchronized { !followerConnections.remove(followerConnection) }
      }
    }
  }

  override def runSynchronous(programAst: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    run(programAst, k, options)

    //FIXME:Don't busy wait
    while (followerConnections synchronized { !followerConnections.isEmpty }) {
      Thread.sleep(150)
    }
    Thread.sleep(150)
    Logger.exiting(getClass.getName, "runSynchronous")
  }

  override def stop() = {
    followerConnections foreach { _.socket.shutdownOutput() }
    followerConnections.clear()
    super.stop()
  }

}

object LeaderRuntime {
  val nextGroupProxyId = new AtomicLong(12)
  val NO_GROUP = 0L
}
