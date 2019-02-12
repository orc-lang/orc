//
// DOrcExecution.scala -- Scala classes DOrcExecution, DOrcLeaderExecution, and DOrcFollowerExecution
// Project OrcScala
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.run.core.{ Execution, Token }
import orc.run.distrib.Logger
import orc.run.distrib.common.{ MashalingAndRemoteRefSupport, RemoteObjectManager, RemoteRefIdManager, ValueMarshaler, PlacementController }

/** Top level Group, associated with a program running in a dOrc runtime
  * engine.  dOrc executions have an ID, the program AST and OrcOptions,
  * etc.
  *
  * Rule of thumb: Orc Executions keep program state and handle engine-internal
  * behavior (Tokens, Groups, Frames, etc.).  External interaction (with the
  * environment) is the bailiwick of Orc Runtimes.
  *
  * An Execution (Distributed Orc program run) is identified by a execution ID
  * UUID. Each runtime participating in an execution is assigned an
  * unique-to-that-execution follower number.  (The leader runtime for an
  * execution uses "follower" number zero.)
  *
  * @author jthywiss
  */
abstract class DOrcExecution(
    val executionId: DOrcExecution#ExecutionId,
    override val followerExecutionNum: Int,
    programAst: DOrcRuntime#ProgramAST,
    options: OrcExecutionOptions,
    eventHandler: OrcEvent => Unit,
    override val runtime: DOrcRuntime)
  extends Execution(programAst, options, eventHandler, runtime)
  /* Implemented interfaces: */
  with MashalingAndRemoteRefSupport[PeerLocation]
  /* Mixed-in implementation fragments: */
  with RemoteObjectManager[PeerLocation]
  with RemoteRefIdManager[PeerLocation]
  with PlacementController[PeerLocation]
  with ValueMarshaler[DOrcExecution, PeerLocation]
  with ExecutionMashaler
  with GroupProxyManager
  with RemoteFutureManager {

  //TODO: Move to superclass
  def runProgram() {
    /* Initial program token */
    val t = new Token(programAst, this)
    runtime.schedule(t)
  }

  override type ExecutionId = String

  /* For now, runtime IDs and Execution follower numbers are the same.  When
   * we host more than one execution in an engine, they will be different. */
  override def locationForFollowerNum(followerNum: Int): PeerLocation = runtime.locationForRuntimeId(new DOrcRuntime.RuntimeId(followerNum))

  def selectLocationForCall(candidateLocations: Set[PeerLocation]): PeerLocation = candidateLocations.head

}

object DOrcExecution {
  def freshExecutionId(): String = java.util.UUID.randomUUID().toString
}

/** DOrcExecution in the dOrc LeaderRuntime.  This is the "true" root group.
  *
  * @author jthywiss
  */
class DOrcLeaderExecution(
    executionId: DOrcExecution#ExecutionId,
    programAst: DOrcRuntime#ProgramAST,
    options: OrcExecutionOptions,
    eventHandler: OrcEvent => Unit,
    runtime: LeaderRuntime)
  extends DOrcExecution(executionId, 0, programAst, options, eventHandler, runtime) {

  protected val startingFollowers = java.util.concurrent.ConcurrentHashMap.newKeySet[DOrcRuntime.RuntimeId]()
  protected val readyFollowers = java.util.concurrent.ConcurrentHashMap.newKeySet[DOrcRuntime.RuntimeId]()
  protected val followerStartWait = new Object()

  def followerStarting(followerNum: DOrcRuntime.RuntimeId): Unit = {
    startingFollowers.add(followerNum)
  }

  def followerReady(followerNum: DOrcRuntime.RuntimeId): Unit = {
    readyFollowers.add(followerNum)
    startingFollowers.remove(followerNum)
    followerStartWait synchronized followerStartWait.notifyAll()
  }

  def awaitAllFollowersReady(): Unit = {
    while (!startingFollowers.isEmpty()) followerStartWait synchronized followerStartWait.wait()
  }

  override def notifyOrc(event: OrcEvent): Unit = {
    super.notifyOrc(event)
    Logger.Downcall.fine(event.toString)
  }

}

/** Execution group to contain migrated tokens.  This is a "fake" root group to
  * anchor the partial group tree in FollowerRuntimes.
  *
  * @author jthywiss
  */
class DOrcFollowerExecution(
    executionId: DOrcExecution#ExecutionId,
    followerExecutionNum: Int,
    programAst: DOrcRuntime#ProgramAST,
    options: OrcExecutionOptions,
    eventHandler: OrcEvent => Unit,
    runtime: FollowerRuntime)
  extends DOrcExecution(executionId, followerExecutionNum, programAst, options, eventHandler, runtime) {

  override def onHalt() { /* Group halts are not significant here, disregard */ }

}
