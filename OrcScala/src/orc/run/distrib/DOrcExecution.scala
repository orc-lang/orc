//
// DOrcExecution.scala -- Scala classes DOrcExecution, DOrcLeaderExecution, and DOrcFollowerExecution
// Project OrcScala
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.ast.oil.nameless.Expression
import orc.run.core.Execution

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
    node: Expression,
    options: OrcExecutionOptions,
    eventHandler: OrcEvent => Unit,
    override val runtime: DOrcRuntime)
  extends Execution(node, options, eventHandler, runtime)
  with ValueLocator
  with ValueMarshaler
  with GroupProxyManager
  with RemoteFutureManager
  with RemoteObjectManager
  with RemoteRefIdManager {

  type ExecutionId = String

  def locationForFollowerNum(followerNum: DOrcRuntime#RuntimeId): PeerLocation = runtime.locationForRuntimeId(followerNum)

  private val hereSet: Set[PeerLocation] = Set(runtime.here)
  override def currentLocations(v: Any) = {
    val cl = v match {
      case rmt: RemoteRef => Set(homeLocationForRemoteRef(rmt.remoteRefId))
      case orc.lib.util.Prompt => Set(locationForFollowerNum(0))
      case _: java.util.concurrent.ConcurrentSkipListSet[_] => Set(locationForFollowerNum(1))
      case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], _) => Set(locationForFollowerNum(1))
      //case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], "clear") => Set(locationForFollowerNum(1))
      //case orc.values.sites.JavaMemberProxy(s: java.util.concurrent.ConcurrentSkipListSet[_], "contains") => Set(locationForFollowerNum(if (s.size < 50) 0 else 1))
      //case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], "add") => Set(locationForFollowerNum(1))
      case _ => hereSet
    }
    // Logger.finer(s"currentLocations($v)=$cl")
    cl
  }
  override def permittedLocations(v: Any): Set[PeerLocation] = {
    val pl = v match {
      case plp: LocationPolicy => plp.permittedLocations()
      case rmt: RemoteRef => Set(homeLocationForRemoteRef(rmt.remoteRefId))
      case orc.lib.util.Prompt => Set(locationForFollowerNum(0))
      case _: java.util.concurrent.ConcurrentSkipListSet[_] => Set(locationForFollowerNum(1))
      case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], _) => Set(locationForFollowerNum(1))
      //case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], "clear") => Set(locationForFollowerNum(1))
      //case orc.values.sites.JavaMemberProxy(s: java.util.concurrent.ConcurrentSkipListSet[_], "contains") => Set(locationForFollowerNum(if (s.size < 50) 0 else 1))
      //case orc.values.sites.JavaMemberProxy(_: java.util.concurrent.ConcurrentSkipListSet[_], "add") => Set(locationForFollowerNum(1))
      case _ => runtime.allLocations
    }
    Logger.finest(s"permittedLocations($v)=$pl")
    pl
  }

}

object DOrcExecution {
  def freshExecutionId() = java.util.UUID.randomUUID().toString
  val noGroupProxyId: DOrcExecution#GroupProxyId = 0L
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
  runtime: LeaderRuntime)
  extends DOrcExecution(executionId, 0, programAst, options, eventHandler, runtime) {

  override def notifyOrc(event: OrcEvent) {
    super.notifyOrc(event)
    Logger.info(event.toString)
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
  programAst: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: FollowerRuntime)
  extends DOrcExecution(executionId, followerExecutionNum, programAst, options, eventHandler, runtime) {

  override def onHalt() { /* Group halts are not significant here, disregard */ }

}
