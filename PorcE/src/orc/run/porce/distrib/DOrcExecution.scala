//
// DOrcExecution.scala -- Scala classes DOrcExecution, DOrcLeaderExecution, and DOrcFollowerExecution
// Project PorcE
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.compile.parse.OrcSourceRange
import orc.compiler.porce.PorcToPorcE
import orc.run.porce.runtime.{ CPSCallResponseHandler, PorcEClosure, PorcEExecution, PorcEExecutionHolder, PorcEExecutionWithLaunch }
import com.oracle.truffle.api.nodes.RootNode
import orc.run.porce.HasId
import com.oracle.truffle.api.frame.VirtualFrame
import orc.PublishedEvent
import orc.run.porce.PorcEUnit

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
  val followerExecutionNum: Int,
  programAst: DOrcRuntime#ProgramAST,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  override val runtime: DOrcRuntime)
  extends PorcEExecution( /* node, options,*/ runtime, eventHandler)
  with DistributedInvocationInterceptor
  with ValueLocator
  with ValueMarshaler
  with CounterProxyManager
  with TerminatorProxyManager
  with RemoteFutureManager
  with RemoteObjectManager
  with RemoteRefIdManager {

  val (programPorceAst, programCallTargetMap) = PorcToPorcE(programAst, new PorcEExecutionHolder(this))

  type ExecutionId = String

  def locationForFollowerNum(followerNum: DOrcRuntime#RuntimeId): PeerLocation = runtime.locationForRuntimeId(followerNum)

  private val hereSet: Set[PeerLocation] = Set(runtime.here)
  override def currentLocations(v: Any): Set[PeerLocation] = {
    val cl = v match {
      //TODO: Replace this with location tracking
      case plp: LocationPolicy => plp.permittedLocations(runtime)
      case rmt: RemoteRef => Set(homeLocationForRemoteRef(rmt.remoteRefId))
      case _ => hereSet
    }
    //Logger.finer(s"currentLocations($v: ${v.getClass.getName})=$cl")
    cl
  }
  override def permittedLocations(v: Any): Set[PeerLocation] = {
    val pl = v match {
      case plp: LocationPolicy => plp.permittedLocations(runtime)
      case rmt: RemoteRef => Set(homeLocationForRemoteRef(rmt.remoteRefId))
      case _ => runtime.allLocations
    }
    //Logger.finest(s"permittedLocations($v: ${v.getClass.getName})=$pl")
    pl
  }

}

object DOrcExecution {
  def freshExecutionId(): String = java.util.UUID.randomUUID().toString
  val noGroupProxyId: RemoteRef#RemoteRefId = 0L
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

  //TODO: Move to superclass
  def runProgram(): Unit = {
    scheduleProgram(programPorceAst, programCallTargetMap)
  }

  override def notifyOrc(event: OrcEvent): Unit = {
    super.notifyOrc(event)
    Logger.info(s"${Console.REVERSED}${event.toString}${Console.RESET}")
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

  val pRootNode = new RootNode(null) with HasId {
    def execute(frame: VirtualFrame): Object = {
      // Skip the first argument since it is our captured value array.
      val v = frame.getArguments()(1)
      notifyOrcWithBoundary(PublishedEvent(v))
      // Token: from initial caller of p.
      // FIXME: c.haltToken()
      PorcEUnit.SINGLETON
    }

    def getId() = -1
  }

  val pCallTarget = truffleRuntime.createCallTarget(pRootNode)

  protected override def setCallTargetMap(callTargetMap: collection.Map[Int, RootCallTarget]) = {
    super.setCallTargetMap(callTargetMap)
    this.callTargetMap += (-1 -> pCallTarget)
  }

  {
    val (_, map) = PorcToPorcE(programAst, new PorcEExecutionHolder(this))

    setCallTargetMap(map)

    Logger.finest(s"Loaded program in follower. CallTagetMap:\n${callTargetMap.mkString("\n")}")
  }
}

case class CallMemento(callSiteId: Int, callSitePosition: Option[OrcSourceRange], publicationContinuation: PorcEClosure, counterProxyId: CounterProxyManager#CounterProxyId, terminatorProxyId: TerminatorProxyManager#TerminatorProxyId, target: AnyRef, arguments: Array[AnyRef]) extends Serializable {
  def this(callHandler: CPSCallResponseHandler, counterProxyId: CounterProxyManager#CounterProxyId, terminatorProxyId: TerminatorProxyManager#TerminatorProxyId, target: AnyRef, arguments: Array[AnyRef]) {
    this(callSiteId = callHandler.callSiteId, callSitePosition = callHandler.callSitePosition, publicationContinuation = callHandler.p, counterProxyId = counterProxyId, terminatorProxyId = terminatorProxyId, target = target, arguments = arguments)
  }
}

case class PublishMemento(publicationContinuation: PorcEClosure, publishedValue: AnyRef) extends Serializable
