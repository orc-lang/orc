//
// FollowerRuntimeCmdLineOptions.scala -- Scala class FollowerRuntimeCmdLineOptions
// Project OrcScala
//
// Created by jthywiss on Feb 10, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

import java.net.InetSocketAddress
import java.nio.file.Path

import orc.util.CmdLineParser

/** TODO:DESCRIBE
  *
  * @author jthywiss
  */
class FollowerRuntimeCmdLineOptions() extends CmdLineParser {
  private var runtimeId_ = 0
  def runtimeId: Int = runtimeId_
  def runtimeId_=(newVal: Int): Unit = runtimeId_ = newVal
  private var leaderSocketAddress_ : InetSocketAddress = null
  def leaderSocketAddress: InetSocketAddress = leaderSocketAddress_
  def leaderSocketAddress_=(newVal: InetSocketAddress): Unit = leaderSocketAddress_ = newVal
  private var listenSocketAddress_ : InetSocketAddress = null
  def listenSocketAddress: InetSocketAddress = listenSocketAddress_
  def listenSocketAddress_=(newVal: InetSocketAddress): Unit = listenSocketAddress_ = newVal
  private var listenSockAddrFile_ : Option[Path] = None
  def listenSockAddrFile: Option[Path] = listenSockAddrFile_
  def listenSockAddrFile_=(newVal: Option[Path]): Unit = listenSockAddrFile_ = newVal

  IntOprd(() => runtimeId, runtimeId = _, position = 0, argName = "runtime-id", required = true, usage = "d-Orc runtime (follower) ID")

  SocketOprd(() => leaderSocketAddress, leaderSocketAddress = _, position = 1, argName = "leader", required = true, usage = "Leader's socket address (host:port) to connect to")

  SocketOpt(() => listenSocketAddress, listenSocketAddress = _, ' ', "listen", usage = "Local socket address (host:port) to listen on. Default is to listen on a random free dynamic port on all local interfaces.")

  FileOpt(() => listenSockAddrFile.getOrElse(null), f => listenSockAddrFile = Some(f), ' ', "listen-sockaddr-file", usage = "Write the actual bound listen socket address to this file. Useful when listening on a random port.")
}
