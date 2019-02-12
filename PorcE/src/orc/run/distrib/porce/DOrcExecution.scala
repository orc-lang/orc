//
// DOrcExecution.scala -- Scala classes DOrcExecution, DOrcLeaderExecution, DOrcFollowerExecution, CallMemento, PublishMemento, and KillingMemento
// Project PorcE
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.{ OrcEvent, OrcExecutionOptions, PublishedEvent }
import orc.compile.parse.OrcSourceRange
import orc.run.distrib.Logger
import orc.run.distrib.common.{ MashalingAndRemoteRefSupport, PlacementController, RemoteObjectManager, RemoteRefIdManager, ValueMarshaler }
import orc.run.porce.{ HasId, PorcEUnit }
import orc.run.porce.runtime.{ MaterializedCPSCallContext, PorcEClosure, PorcEExecution, PorcEExecutionWithLaunch }

import com.oracle.truffle.api.RootCallTarget
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.RootNode

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
  extends PorcEExecution( /* programAst, */ runtime, eventHandler, options)
  /* Implemented interfaces: */
  with MashalingAndRemoteRefSupport[PeerLocation]
  /* Mixed-in implementation fragments: */
  with RemoteObjectManager[PeerLocation]
  with RemoteRefIdManager[PeerLocation]
  with PlacementController[PeerLocation]
  with ValueMarshaler[DOrcExecution, PeerLocation]
  with DistributedInvocationInterceptor
  with ExecutionMashaler
  with CounterProxyManager
  with TerminatorProxyManager
  with RemoteFutureManager {

  Logger.ProgLoad.fine("PorcToPorcE compile start")
  val startTime = System.nanoTime
  val (programPorceAst, programCallTargetMap) = porcToPorcE.method(programAst)
  Logger.ProgLoad.fine(s"PorcToPorcE compile finish (compile duration ${(System.nanoTime - startTime) / 1.0e9} s)")

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
  extends DOrcExecution(executionId, 0, programAst, options, eventHandler, runtime) with PorcEExecutionWithLaunch {

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

  //TODO: Move to superclass
  def runProgram(): Unit = {
    scheduleProgram(programPorceAst, programCallTargetMap)
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
    rootCounterId: CounterProxyManager#DistributedCounterId,
    rootCounterOrigin: PeerLocation,
    runtime: FollowerRuntime)
  extends DOrcExecution(executionId, followerExecutionNum, programAst, options, eventHandler, runtime) {

  // Token: This does not get a token initially.
  val counter = getDistributedCounterForId(rootCounterId).counter

  val pRootNode = new RootNode(null) with HasId {
    override def execute(frame: VirtualFrame): Object = {
      // Skip the first argument since it is our captured value array.
      val v = frame.getArguments()(1)
      notifyOrcWithBoundary(PublishedEvent(v))
      // Token: from caller of P.
      counter.haltToken()
      PorcEUnit.SINGLETON
    }

    override def getId() = -1
  }

  val pCallTarget = truffleRuntime.createCallTarget(pRootNode)

  override def kill(): Unit = {
    //TODO: Forward the kill to the leader execution
    // ... maybe.  For Java Errors and TokenErrors, the event has already been sent to the leader.
  }

  protected override def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    super.setCallTargetMap(callTargetMap)
    this.callTargetMap += (-1 -> pCallTarget)
  }

  {
    val (_, map) = porcToPorcE.method(programAst)

    setCallTargetMap(map)

    //Logger.ProgLoad.finest(s"Loaded program in follower. CallTagetMap:\n${callTargetMap.mkString("\n")}")
  }
}

case class CallMemento(callSiteId: Int, callSitePosition: Option[OrcSourceRange], publicationContinuation: PorcEClosure, counterId: CounterProxyManager#DistributedCounterId, credit: Int, terminatorProxyId: TerminatorProxyManager#TerminatorProxyId, target: AnyRef, arguments: Array[AnyRef]) extends Serializable {
  def this(callContext: MaterializedCPSCallContext, counterId: CounterProxyManager#DistributedCounterId, credit: Int, terminatorProxyId: TerminatorProxyManager#TerminatorProxyId, target: AnyRef, arguments: Array[AnyRef]) {
    this(callSiteId = callContext.callSiteId, callSitePosition = callContext.callSitePosition, publicationContinuation = callContext.p, counterId = counterId, credit = credit, terminatorProxyId = terminatorProxyId, target = target, arguments = arguments)
  }
}

case class PublishMemento(publicationContinuation: PorcEClosure, publishedValue: AnyRef) extends Serializable

case class KillingMemento(counterId: CounterProxyManager#DistributedCounterId, credit: Int, continuation: PorcEClosure) extends Serializable
