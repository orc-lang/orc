//
// OrcCmd.scala -- Scala trait OrcCmd
// Project OrcScala
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.OrcExecutionOptions

/** Command sent to dOrc follower runtime engines.
  *
  * @author jthywiss
  */
trait OrcCmd extends Serializable
case class LoadProgramCmd(programId: LeaderRuntime#ExecutionId, programOil: String, options: OrcExecutionOptions) extends OrcCmd
case class UnloadProgramCmd(programId: LeaderRuntime#ExecutionId) extends OrcCmd


/** Command sent to dOrc follower runtime engines from leader or other followers.
  *
  * @author jthywiss
  */
trait OrcPeerCmd extends OrcCmd
case class HostTokenCmd(programId: LeaderRuntime#ExecutionId, movedToken: TokenReplacement) extends OrcPeerCmd
case class KillGroupCmd(groupProxyId: LeaderRuntime#GroupProxyId) extends OrcPeerCmd
