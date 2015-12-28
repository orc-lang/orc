//
// FollowerRuntime.scala -- Scala class FollowerRuntime
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

import java.io.IOException
import java.net.InetSocketAddress

import scala.ref.WeakReference
import scala.xml.XML

import orc.{ CaughtEvent, OrcEvent, OrcExecutionOptions, OrcRuntime }
import orc.ast.oil.nameless.Expression
import orc.ast.oil.xml.OrcXML
import orc.run.StandardOrcRuntime
import orc.run.core.Execution
import orc.run.extensions.SupportForDOrc
import orc.util.{ ConnectionListener, SocketObjectConnection }

/** Orc runtime engine running as part of a dOrc cluster.
  *
  * @author jthywiss
  */
class FollowerRuntime(listenAddress: InetSocketAddress) extends StandardOrcRuntime("dOrc @ " + listenAddress.toString()) with
  SupportForDOrc {

  def listen() {
    Logger.info(s"Listening on $listenAddress")
    val listener = new ConnectionListener[OrcCmd, (LeaderRuntime#GroupProxyId, OrcEvent)](listenAddress)
    try {
      val newConnection = listener.acceptConnection()
      Logger.finer(s"accepted ${newConnection.socket}")
      try {
        followDOrcLeader(newConnection)
      } catch {
        case e: InterruptedException => throw e
        case e: Throwable => { newConnection.send("", new CaughtEvent(e)); throw e }
      } finally {
        newConnection.close()
      }
    } catch {
      case (e: IOException) => Logger.finer(s"Ignoring $e") /* Ignore close failures at this point */
    } finally {
      listener.close()
      Logger.info(s"Closed $listenAddress")
    }
  }

  def followDOrcLeader(leaderConnection: SocketObjectConnection[OrcCmd, (LeaderRuntime#GroupProxyId, OrcEvent)]) {
    while (!leaderConnection.closed && !leaderConnection.socket.isInputShutdown) {
      val cmd = leaderConnection.receive()
      Logger.finest(s"received $cmd")
      cmd match {
        case LoadProgramCmd(id, oil, options) => loadProgram(leaderConnection, id, oil, options)
        case HostTokenCmd(id, movedToken) => hostToken(leaderConnection, id, movedToken)
        case KillGroupCmd(groupId) => killGroup(groupId)
        case UnloadProgramCmd(id) => { unloadProgram(id); leaderConnection.close() }
      }
    }
  }

  val programs = new java.util.concurrent.ConcurrentHashMap[LeaderRuntime#ExecutionId, DOrcFollowerExecution]

  def loadProgram(leaderConnection: SocketObjectConnection[OrcCmd, (LeaderRuntime#GroupProxyId, OrcEvent)], executionId: LeaderRuntime#ExecutionId, programOil: String, options: OrcExecutionOptions) {
    Logger.entering(getClass.getName, "loadProgram")
    assert(programs.isEmpty()) /* For now */
    if (programs.isEmpty()) {
      Logger.fine(s"starting scheduler")
      startScheduler(options)
    }

    val programAst = OrcXML.xmlToAst(XML.loadString(programOil))
    val root = new DOrcFollowerExecution(programAst, options, { e => Logger.finer(s"eventHandler: sending ${(("", e))}"); leaderConnection.send(("", e)) }, this)
    installHandlers(root)

    programs.put(executionId, root)
    roots.put(new WeakReference(root), ())
  }

  val proxiedGroups = new java.util.concurrent.ConcurrentHashMap[LeaderRuntime#GroupProxyId, RemoteGroupProxy]

  def hostToken(leaderConnection: SocketObjectConnection[OrcCmd, (LeaderRuntime#GroupProxyId, OrcEvent)], executionId: LeaderRuntime#ExecutionId, movedToken: TokenReplacement) {
    Logger.entering(getClass.getName, "hostToken", Seq(leaderConnection, executionId, movedToken))
    val newGroup = new RemoteGroupProxy(this, programs.get(executionId).options, { e => Logger.finer(s"sendEventFunc: sending ${((movedToken.tokenProxyId, e))}"); leaderConnection.send((movedToken.tokenProxyId, e)) })
    proxiedGroups.put(movedToken.tokenProxyId, newGroup)
    val newToken = movedToken.asToken(programs.get(executionId).node, newGroup)
    Logger.fine(s"scheduling $newToken")
    schedule(newToken)
  }

  def killGroup(groupId: LeaderRuntime#GroupProxyId) = proxiedGroups.get(groupId).kill()

  def unloadProgram(executionId: LeaderRuntime#ExecutionId) {
    Logger.entering(getClass.getName, "unloadProgram", Seq(executionId))
    programs.remove(executionId)

    assert(programs.isEmpty()) /* For now */
    if (programs.isEmpty()) {
      stopScheduler()
      super.stop()
    }
  }
}

object FollowerRuntime {

  def main(args: Array[String]) {
    assert(args.length == 1)
    new FollowerRuntime(new InetSocketAddress(args(0).toInt)).listen()
  }

}


/** Execution group to contain migrated tokens.
  *
  * @author jthywiss
  */
class DOrcFollowerExecution(
  node: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: OrcRuntime)
  extends Execution(node, options, eventHandler, runtime) {

  override def onHalt() { /* Group halts are not significant here, disregard */ }

}
