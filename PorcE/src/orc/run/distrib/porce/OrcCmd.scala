//
// OrcCmd.scala -- Scala trait OrcCmd and its subclasses
// Project PorcE
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import java.net.InetSocketAddress

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.run.distrib.common.{ ExecutionContextSerializationMarker, RemoteRef }

/** Command sent to dOrc runtime engines.
  *
  * @author jthywiss
  */
abstract sealed class OrcCmd() extends Serializable

abstract sealed trait OrcLeaderToFollowerCmd extends OrcCmd
abstract sealed trait OrcFollowerToLeaderCmd extends OrcCmd
abstract sealed trait OrcPeerCmd extends OrcLeaderToFollowerCmd with OrcFollowerToLeaderCmd

abstract sealed class OrcFollowerToLeaderCmdInExecutionContext(executionId: DOrcExecution#ExecutionId) extends OrcFollowerToLeaderCmd {
  val xid = new ExecutionContextSerializationMarker(executionId)
}

abstract sealed class OrcPeerCmdInExecutionContext(executionId: DOrcExecution#ExecutionId) extends OrcPeerCmd {
  val xid = new ExecutionContextSerializationMarker(executionId)
}

////////
// Inform runtime instances of each other
////////

/** A new peer has joined this cluster */
case class AddPeerCmd(peerRuntimeId: DOrcRuntime.RuntimeId, peerListenAddress: InetSocketAddress) extends OrcLeaderToFollowerCmd {
  override def toString: String = s"$productPrefix($peerRuntimeId, $peerListenAddress)"
}

/** A peer has left this cluster */
case class RemovePeerCmd(peerRuntimeId: DOrcRuntime.RuntimeId) extends OrcLeaderToFollowerCmd {
  override def toString: String = s"$productPrefix($peerRuntimeId)"
}

////////
// Begin and end executions
////////

/** Prepare for execution in the given Orc program AST */
case class LoadProgramCmd(executionId: DOrcExecution#ExecutionId, programAst: DOrcRuntime#ProgramAST, options: OrcExecutionOptions, rootCounterId: CounterProxyManager#DistributedCounterId) extends OrcLeaderToFollowerCmd {
  override def toString: String = s"$productPrefix($executionId, ...programAst.., $options)"
}

/** This runtime is ready to execute in the indicated executionId */
case class ProgramReadyCmd(executionId: DOrcExecution#ExecutionId) extends OrcFollowerToLeaderCmd {
  override def toString: String = s"$productPrefix($executionId)"
}

/** No further execution in this executionId, program AST can be discarded  */
case class UnloadProgramCmd(executionId: DOrcExecution#ExecutionId) extends OrcLeaderToFollowerCmd {
  override def toString: String = s"$productPrefix($executionId)"
}

////////
// Notify leader (and therefore the environment) of events
////////

/** Notify leader/environment of an OrcEvent */
case class NotifyLeaderCmd(executionId: DOrcExecution#ExecutionId, event: OrcEvent) extends OrcFollowerToLeaderCmdInExecutionContext(executionId) {
  override def toString: String = s"$productPrefix($executionId, $event)"
}

////////
// Connection management messages
////////

/** Header -- always the first message on a new connection */
case class DOrcConnectionHeader(sendingRuntimeId: DOrcRuntime.RuntimeId, receivingRuntimeId: DOrcRuntime.RuntimeId, senderListenSocketAddress: InetSocketAddress) extends OrcPeerCmd {
  override def toString: String = s"$productPrefix($sendingRuntimeId, $receivingRuntimeId, $senderListenSocketAddress)"
}

////////
// Moving tokens, reporting group and member status, reading futures
////////

/** Execute this token */
case class MigrateCallCmd(executionId: DOrcExecution#ExecutionId, tokenId: Long, movedCall: CallMemento) extends OrcPeerCmdInExecutionContext(executionId) {
  // Token: This message carries a token on the counter movedCall.counterProxyId at the origin. (the location is important)
  override def toString: String = f"$productPrefix($executionId, $tokenId%#x, $movedCall)"
}

/** This token has published in the given group */
// TODO: This can probably be removed for PorcE.
case class PublishGroupCmd(executionId: DOrcExecution#ExecutionId, counterId: CounterProxyManager#CounterId, publication: PublishMemento) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $counterId%#x, $publication)"
}

/** Kill the given group which has already been killed at it's origin */
case class KilledGroupCmd(executionId: DOrcExecution#ExecutionId, counterId: RemoteRef#RemoteRefId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $counterId%#x)"
}

/** Notify the given group that an event has happened that will kill if it is not already dead. */
case class KillingGroupCmd(executionId: DOrcExecution#ExecutionId, counterId: RemoteRef#RemoteRefId, killing: KillingMemento) extends OrcPeerCmdInExecutionContext(executionId) {
  // Credit: This message carries credit on the counter killing.counterProxyId.
  override def toString: String = f"$productPrefix($executionId, $counterId%#x, $killing)"
}

/** This group member has halted */
case class HaltGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, counterId: CounterProxyManager#CounterId, credit: Int) extends OrcPeerCmdInExecutionContext(executionId) {
  // Credit: This message carries credit on the counter counterId at the destination. (the location is important)
  override def toString: String = f"$productPrefix($executionId, $counterId%#x, $credit)"
}

/** This group member has discorporated */
case class DiscorporateGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, counterId: CounterProxyManager#CounterId, credit: Int) extends OrcPeerCmdInExecutionContext(executionId) {
  // Credit: This message carries credit on the counter counterId at the destination. (the location is important)
  override def toString: String = f"$productPrefix($executionId, $counterId%#x, $credit)"
}

/** This group member has resurrected */
case class ResurrectGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, counterId: CounterProxyManager#CounterId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $counterId%#x)"
}

/** Provide credit to a remote counter fragment.
  *
  * This message is sent in reply to ResurrectGroupMemberProxyCmd.
  */
case class ProvideCounterCreditCmd(executionId: DOrcExecution#ExecutionId, counterId: CounterProxyManager#CounterId, credit: Int) extends OrcPeerCmdInExecutionContext(executionId) {
  // Credit: This message carries credit on the counter counterId at the **origin**. (the location is important)
  override def toString: String = f"$productPrefix($executionId, $counterId%#x, $credit)"
}

/** Post a read request on this future */
case class ReadFutureCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureManager#RemoteFutureId, readerFollowerRuntimeId: DOrcRuntime.RuntimeId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $futureId%#x, $readerFollowerRuntimeId)"
}

/** The given future has resolved to a value */
case class DeliverFutureResultCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureManager#RemoteFutureId, value: Option[AnyRef]) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $futureId%#x, $value)"
}

/** Resolve this future to the given value */
case class ResolveFutureCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureManager#RemoteFutureId, value: Option[AnyRef]) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString: String = f"$productPrefix($executionId, $futureId%#x, $value)"
}

////////
// Psuedo-messages (not transmitted; synthesized by RuntimeConnection)
////////

/** Psuedo-message used to indicate a closed connection */
case object EOF extends OrcPeerCmd
