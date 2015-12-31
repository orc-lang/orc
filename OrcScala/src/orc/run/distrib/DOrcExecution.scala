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

import orc.{ OrcEvent, OrcExecutionOptions, OrcRuntime, Schedulable }
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

    destination.send(HostTokenCmd(executionId, new TokenReplacement(token, programAst, rmtProxy.thisProxyId)))
  }

  def hostToken(origin: Location, movedToken: TokenReplacement) {
    Logger.entering(getClass.getName, "hostToken", Seq(origin, movedToken))
    val lookedUpProxyGroupMember = proxiedGroupMembers.get(movedToken.tokenProxyId)
    val newTokenGroup = lookedUpProxyGroupMember match {
      case null => { /* Not a token we've seen before */
        val rgp = new RemoteGroupProxy(this, movedToken.tokenProxyId, sendPublish(origin, movedToken.tokenProxyId), sendHalt(origin, movedToken.tokenProxyId))
        proxiedGroups.put(movedToken.tokenProxyId, rgp)
        rgp
      }
      case gmp => gmp.parent
    }
    val newToken = movedToken.asToken(this.node, newTokenGroup)
    if (lookedUpProxyGroupMember != null) {
      /* Discard unused RemoteGroupMenbersProxy */
      origin.send(HaltGroupMemberProxyCmd(executionId, movedToken.tokenProxyId))
    }
    Logger.fine(s"scheduling $newToken")
    runtime.schedule(newToken)
  }

  def sendPublish(destination: Location, proxyId: GroupProxyId)(token: Token, v: Option[AnyRef]) {
    destination.send(PublishGroupCmd(executionId, proxyId, new TokenReplacement(token, programAst, proxyId), v))
  }

  def publishInGroup(groupMemberProxyId: GroupProxyId, publishingToken: TokenReplacement, v: Option[AnyRef]) {
    Logger.entering(getClass.getName, "publishInGroup", Seq(groupMemberProxyId.toString, publishingToken, v))
    val newTokenGroup = proxiedGroupMembers.get(publishingToken.tokenProxyId).parent
    val newToken = publishingToken.asPublishingToken(this.node, newTokenGroup, v)
    Logger.fine(s"scheduling $newToken")
    runtime.schedule(newToken)
  }

  def sendHalt(destination: Location, groupMemberProxyId: GroupProxyId)() {
    destination.send(HaltGroupMemberProxyCmd(executionId, groupMemberProxyId))
  }

  def haltGroupMemberProxy(groupMemberProxyId: GroupProxyId) {
    Logger.entering(getClass.getName, "haltGroupMemberProxy", Seq(groupMemberProxyId.toString))
    val g = proxiedGroupMembers.get(groupMemberProxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.halt() } })
      proxiedGroupMembers.remove(groupMemberProxyId)
    } else {
      Logger.fine(s"halt group member proxy on unknown group member proxy $groupMemberProxyId")
    }
  }

  def sendKill(destination: Location, proxyId: GroupProxyId)() {
    destination.send(KillGroupCmd(executionId, proxyId))
  }

  def killGroupProxy(proxyId: GroupProxyId) {
    Logger.entering(getClass.getName, "killGroupProxy", Seq(proxyId.toString))
    val g = proxiedGroups.get(proxyId)
    if (g != null) {
      runtime.schedule(new Schedulable { def run() = { g.kill() } })
      proxiedGroups.remove(proxyId)
    } else {
      Logger.fine(s"kill group on unknown group $proxyId")
    }
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
