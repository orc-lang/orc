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

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.ast.oil.nameless.Expression
import orc.run.core.Execution

/** Top level Group, associated with a program runnning in a dOrc runtime
  * engine.  dOrc executions have an ID, the program AST and OrcOptions,
  * etc.
  *
  * Rule of thumb: Orc Executions keep program state and handle engine-internal
  * behavior (Tokens, Groups, Frames, etc.).  External interaction (with the
  * environment) is the bailiwick of Orc Runtimes.
  *
  * Am Execution (Distributer Orc progrm run) is identified by a execution ID
  * UUID. Each runtime particiapting in an execution is assigned an
  * unique-to-that-execution follower number.  (The leader runtime for an
  * execution uses "follower" number zero.)
  *
  * @author jthywiss
  */
class DOrcExecution(
  val executionId: DOrcExecution#ExecutionId,
  val followerExecutionNum: Int,
  override val node: Expression,
  options: OrcExecutionOptions,
  eventHandler: OrcEvent => Unit,
  runtime: DOrcRuntime)
  extends Execution(node, options, eventHandler, runtime)
  with GroupProxyManager with RemoteFutureManager with RemoteRefIdManager {

  type ExecutionId = String

  def locationForFollowerNum(followerNum: Int): Location = runtime.followerNumLocationMap.get(followerNum)

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
  runtime: DOrcRuntime)
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
  runtime: DOrcRuntime)
  extends DOrcExecution(executionId, followerExecutionNum, programAst, options, eventHandler, runtime) {

  override def onHalt() { /* Group halts are not significant here, disregard */ }

}
