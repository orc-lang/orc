//
// DOrcExecution.scala -- Scala classes DOrcExecution, DOrcLeaderExecution, and DOrcFollowerExecution
// Project OrcScala
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import java.util.concurrent.atomic.AtomicLong

import orc.{ OrcEvent, OrcExecutionOptions, OrcRuntime }
import orc.ast.oil.nameless.Expression
import orc.run.core.{ Execution, Token }

/** Top level Group, associated with a program runnning in a dOrc runtime
  * engine.  dOrc executions have an ID, the program AST and OrcOptions,
  * etc.
  *
  * Rule of thumb: Orc Executions keep program state and handle engine-internal
  * behavior (Tokens, Groups, Frames, etc.).  External interaction (with the
  * environment) is the bailiwick of Orc Runtimes.
  *
  * @author jthywiss
  */
class DOrcExecution(
  val executionId: DOrcExecution#ExecutionId,
  val followerExecutionNum: Int,
  programAst: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: OrcRuntime)
  extends Execution(programAst, options, eventHandler, runtime) {

  type ExecutionId = String
  type GroupProxyId = Long

  require(followerExecutionNum < 922337203)
  private val groupProxyIdCounter = new AtomicLong(10000000000L * followerExecutionNum + 1L)
  def freshGroupProxyId(): GroupProxyId = groupProxyIdCounter.getAndIncrement()

  val proxiedGroups = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupProxy]
  val proxiedGroupMembers = new java.util.concurrent.ConcurrentHashMap[GroupProxyId, RemoteGroupMembersProxy]

  def sendToken(token: Token, destination: Location) {
    val group = token.getGroup
    val proxyId = group match {
      case rgp: RemoteGroupProxy => rgp.remoteProxyId
      case _ => freshGroupProxyId()
    }
    val rmtProxy = new RemoteGroupMembersProxy(group, sendKill(destination, proxyId), proxyId)
    proxiedGroupMembers.put(proxyId, rmtProxy)

    group.add(rmtProxy)
    group.remove(token)

    destination.send(HostTokenCmd(executionId, new TokenReplacement(token, programAst, rmtProxy)))
  }

  def hostToken(origin: Location, movedToken: TokenReplacement) {
    Logger.entering(getClass.getName, "hostToken", Seq(origin, movedToken))
    val newTokenGroup = proxiedGroupMembers.get(movedToken.tokenProxyId) match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(this, movedToken.tokenProxyId, { e => origin.send(NotifyGroupCmd(executionId, movedToken.tokenProxyId, e)) } )
        proxiedGroups.put(movedToken.tokenProxyId, rgp)
        rgp
      }
      case gmp => gmp.parent
    }
    val newToken = movedToken.asToken(this.node, newTokenGroup)
    Logger.fine(s"scheduling $newToken")
    runtime.schedule(newToken)
  }

  def killGroupProxy(proxyId: GroupProxyId) {
    proxiedGroups.get(proxyId).kill()
    proxiedGroups.remove(proxyId)
  }

  def sendKill(destination: Location, proxyId: GroupProxyId)() {
    destination.send(KillGroupCmd(executionId, proxyId))
  }
}

object DOrcExecution {
  def freshExecutionId() = java.util.UUID.randomUUID().toString
  val NoGroupProxyId: DOrcExecution#GroupProxyId = 0L
}

/** DOrcExecution in the dOrc LeaderRuntime.  This is the "true" root group.
  *
  * @author jthywiss
  */
class DOrcLeaderExecution(
  executionId: DOrcExecution#ExecutionId,
  programAst: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: OrcRuntime)
  extends DOrcExecution(executionId, 0, programAst, options, eventHandler, runtime) {

}

/** Execution group to contain migrated tokens.  This is a "fake" root group to
  * anchor the partial group tree in FollowerRuntimes.
  *
  * @author jthywiss
  */
class DOrcFollowerExecution(
  executionId: DOrcExecution#ExecutionId,
  followerExecutionNum: Int,
  programAst: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: OrcRuntime)
  extends DOrcExecution(executionId, followerExecutionNum, programAst, options, eventHandler, runtime) {

  override def onHalt() { /* Group halts are not significant here, disregard */ }

}
