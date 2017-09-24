//
// OrcCmd.scala -- Scala trait OrcCmd and its subclasses
// Project OrcScala
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import java.net.InetSocketAddress

import orc.{ OrcEvent, OrcExecutionOptions }

/** Command sent to dOrc runtime engines.
  *
  * @author jthywiss
  */
abstract sealed class OrcCmd() extends Serializable

abstract sealed trait OrcLeaderToFollowerCmd extends OrcCmd
abstract sealed trait OrcFollowerToLeaderCmd extends OrcCmd
abstract sealed trait OrcPeerCmd extends OrcLeaderToFollowerCmd with OrcFollowerToLeaderCmd

abstract sealed class OrcPeerCmdInExecutionContext(executionId: DOrcExecution#ExecutionId) extends OrcPeerCmd {
  val xid = new ExecutionContextSerializationMarker(executionId)
}

////////
// Inform runtime instances of each other
////////

/** A new peer has joined this cluster */
case class AddPeerCmd(peerRuntimeId: DOrcRuntime#RuntimeId, peerListenAddress: InetSocketAddress) extends OrcLeaderToFollowerCmd {
  override def toString(): String = s"${getClass.getSimpleName}($peerRuntimeId, $peerListenAddress)"
}

/** A peer has left this cluster */
case class RemovePeerCmd(peerRuntimeId: DOrcRuntime#RuntimeId) extends OrcLeaderToFollowerCmd {
  override def toString(): String = s"${getClass.getSimpleName}($peerRuntimeId)"
}

////////
// Begin and end executions
////////

/** Prepare for execution in the given Orc program AST */
case class LoadProgramCmd(executionId: DOrcExecution#ExecutionId, programOil: String, options: OrcExecutionOptions) extends OrcLeaderToFollowerCmd {
  override def toString(): String = s"${getClass.getSimpleName}($executionId,...programOil..,$options)"
}

/** No further execution in this executionId, program AST can be discarded  */
case class UnloadProgramCmd(executionId: DOrcExecution#ExecutionId) extends OrcLeaderToFollowerCmd {
  override def toString(): String = s"${getClass.getSimpleName}($executionId)"
}

////////
// Notify leader (and therefore the environment) of events
////////

/** Notify leader/environment of an OrcEvent */
case class NotifyLeaderCmd(executionId: DOrcExecution#ExecutionId, event: OrcEvent) extends OrcFollowerToLeaderCmd {
  override def toString(): String = s"${getClass.getSimpleName}($executionId,$event)"
}

////////
// Connection management messages
////////

/** Header -- always the first message on a new connection */
case class DOrcConnectionHeader(sendingRuntimeId: DOrcRuntime#RuntimeId, receivingRuntimeId: DOrcRuntime#RuntimeId) extends OrcPeerCmd {
  override def toString(): String = f"${getClass.getSimpleName}($sendingRuntimeId%#x,$receivingRuntimeId%#x)"
}

////////
// Moving tokens, reporting group and member status, reading futures
////////

/** Execute this token */ 
case class HostTokenCmd(executionId: DOrcExecution#ExecutionId, movedToken: TokenReplacement) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = s"${getClass.getSimpleName}($executionId, $movedToken)"
}

/** This token has published in the given group */ 
case class PublishGroupCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId, publishingToken: PublishingTokenReplacement) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$groupMemberProxyId%#x, $publishingToken)"
}

/** Kill the given group */ 
case class KillGroupCmd(executionId: DOrcExecution#ExecutionId, groupProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$groupProxyId%#x)"
}

/** This group member has halted */ 
case class HaltGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$groupMemberProxyId%#x)"
}

/** This group member has discorporated */ 
case class DiscorporateGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$groupMemberProxyId%#x)"
}

/** Post a read request on this future */
case class ReadFutureCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureRef#RemoteRefId, readerFollowerNum: Int) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$futureId%#x,$readerFollowerNum)"
}

/** The given future has resolved to a value */
case class DeliverFutureResultCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) extends OrcPeerCmdInExecutionContext(executionId) {
  override def toString(): String = f"${getClass.getSimpleName}($executionId,$futureId%#x,$value)"
}

////////
// Psuedo-messages (not transmitted; synthesized by RuntimeConnection)
////////

/** Psuedo-message used to indicate a closed connection */
case object EOF extends OrcPeerCmd
