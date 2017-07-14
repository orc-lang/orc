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

package orc.run.distrib

import java.net.InetSocketAddress

import orc.{ OrcEvent, OrcExecutionOptions }

/** Command sent to dOrc runtime engines.
  *
  * @author jthywiss
  */
sealed abstract class OrcCmd() extends Serializable

sealed trait OrcLeaderToFollowerCmd extends OrcCmd
sealed trait OrcFollowerToLeaderCmd extends OrcCmd
sealed trait OrcPeerCmd extends OrcLeaderToFollowerCmd with OrcFollowerToLeaderCmd

sealed abstract class OrcPeerCmdInExecutionContext(executionId: DOrcExecution#ExecutionId) extends OrcPeerCmd {
  val xid = new ExecutionContextSerializationMarker(executionId)
}

case class AddPeerCmd(peerRuntimeId: DOrcRuntime#RuntimeId, peerListenAddress: InetSocketAddress) extends OrcLeaderToFollowerCmd
case class RemovePeerCmd(peerRuntimeId: DOrcRuntime#RuntimeId) extends OrcLeaderToFollowerCmd

case class LoadProgramCmd(executionId: DOrcExecution#ExecutionId, programOil: String, options: OrcExecutionOptions) extends OrcLeaderToFollowerCmd
case class UnloadProgramCmd(executionId: DOrcExecution#ExecutionId) extends OrcLeaderToFollowerCmd

case class NotifyLeaderCmd(executionId: DOrcExecution#ExecutionId, event: OrcEvent) extends OrcFollowerToLeaderCmd

case class DOrcConnectionHeader(sendingRuntimeId: DOrcRuntime#RuntimeId, receivingRuntimeId: DOrcRuntime#RuntimeId) extends OrcPeerCmd
case class HostTokenCmd(executionId: DOrcExecution#ExecutionId, movedToken: TokenReplacement) extends OrcPeerCmdInExecutionContext(executionId)
case class PublishGroupCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId, publishingToken: PublishingTokenReplacement) extends OrcPeerCmdInExecutionContext(executionId)
case class KillGroupCmd(executionId: DOrcExecution#ExecutionId, groupProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId)
case class HaltGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId)
case class DiscorporateGroupMemberProxyCmd(executionId: DOrcExecution#ExecutionId, groupMemberProxyId: DOrcExecution#GroupProxyId) extends OrcPeerCmdInExecutionContext(executionId)
case class ReadFutureCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureRef#RemoteRefId, readerFollowerNum: Int) extends OrcPeerCmdInExecutionContext(executionId)
case class DeliverFutureResultCmd(executionId: DOrcExecution#ExecutionId, futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) extends OrcPeerCmdInExecutionContext(executionId)

case object EOF extends OrcPeerCmd
