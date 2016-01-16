//
// FollowerRuntime.scala -- Scala class FollowerRuntime
// Project OrcScala
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import java.io.{ EOFException, IOException }
import java.net.{ InetSocketAddress, SocketException }

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.ref.WeakReference
import scala.xml.XML

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.ast.oil.xml.OrcXML
import orc.error.loadtime.OilParsingException
import orc.util.{ ConnectionListener, SocketObjectConnection }

/** Orc runtime engine running as part of a dOrc cluster.
  *
  * @author jthywiss
  */
class FollowerRuntime(listenAddress: InetSocketAddress) extends DOrcRuntime("dOrc @ " + listenAddress.toString()) {

  type MsgToFollower = OrcLeaderToFollowerCmd

  protected var leaderLocation: LeaderLocation = null
  override protected val followerNumLocationMap = new java.util.concurrent.ConcurrentHashMap[Int, Location]()

  def listen() {
    Logger.info(s"Listening on $listenAddress")
    val listener = new ConnectionListener[MsgToFollower, LeaderRuntime#MsgToLeader](listenAddress)
    try {
      leaderLocation = new LeaderLocation(listener.acceptConnection())
      Logger.finer(s"accepted ${leaderLocation.connection.socket}")
      try {
        followDOrcLeader(leaderLocation)
      } finally {
        if (!leaderLocation.connection.closed) { Logger.fine(s"followDOrcLeader returned, closing $leaderLocation"); leaderLocation.connection.close() }
        leaderLocation = null
      }
    } catch {
      case (e: IOException) => Logger.finer(s"Ignoring $e") /* Ignore close failures at this point */
    } finally {
      listener.close()
      Logger.info(s"Closed $listenAddress")
    }
  }

  def followDOrcLeader(leaderLocation: LeaderLocation) {
    Logger.entering(getClass.getName, "followDOrcLeader")
    try {
      var done = false

      //FIXME: Add all other followers
      followerNumLocationMap.put(0, leaderLocation)

      while (!done && !leaderLocation.connection.closed && !leaderLocation.connection.socket.isInputShutdown) {
        val cmd = try {
          leaderLocation.connection.receive()
        } catch {
          case _: EOFException => EOF
        }
        Logger.finest(s"received $cmd")
        cmd match {
          case LoadProgramCmd(xid, followerExecutionNum, oil, options) => loadProgram(leaderLocation, xid, followerExecutionNum, oil, options)
          case HostTokenCmd(xid, movedToken) => programs.get(xid).hostToken(leaderLocation, movedToken)
          case PublishGroupCmd(xid, gmpid, t, v) => programs.get(xid).publishInGroup(leaderLocation, gmpid, t, v)
          case HaltGroupMemberProxyCmd(xid, gmpid) => programs.get(xid).haltGroupMemberProxy(gmpid)
          case KillGroupCmd(xid, gpid) => programs.get(xid).killGroupProxy(gpid)
          case ReadFutureCmd(xid, futureId, readerFollowerNum) => programs.get(xid).readFuture(futureId, readerFollowerNum)
          case DeliverFutureResultCmd(xid, futureId, value) => programs.get(xid).deliverFutureResult(futureId, value)
          case UnloadProgramCmd(xid) => { unloadProgram(xid); done = true }
          case EOF => done = true
        }
      }
    } finally {
      if (!programs.isEmpty) Logger.warning(s"Shutting down with ${programs.size} programs still loaded: ${programs.keys.mkString(",")}")
      programs.clear()
      stopScheduler()
      super.stop()
      followerNumLocationMap.clear()
    }
    Logger.exiting(getClass.getName, "followDOrcLeader")
  }

  def sendEvent(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId, groupProxyId: DOrcExecution#GroupProxyId)(event: OrcEvent) {
    Logger.entering(getClass.getName, "sendEvent")
    try {
      leaderLocation.connection.send(NotifyLeaderCmd(executionId, event))
    } catch {
      case e1: SocketException => {
        if (!leaderLocation.connection.closed) {
          try {
            Logger.fine(s"SocketException, aborting $leaderLocation")
            leaderLocation.connection.abort()
          } catch {
            case (e2: IOException) => Logger.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
          throw e1
        } else {
          Logger.finer(s"Ignoring $e1") /* Ignore failures on a closed socket */
        }
      }
    }
    Logger.exiting(getClass.getName, "sendEvent")
  }

  val programs = new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcFollowerExecution]

  def loadProgram(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId, followerExecutionNum: Int, programOil: String, options: OrcExecutionOptions) {
    Logger.entering(getClass.getName, "loadProgram")

    followerNumLocationMap.put(followerExecutionNum, here)

    assert(programs.isEmpty()) /* For now */
    if (programs.isEmpty()) {
      Logger.fine(s"starting scheduler")
      startScheduler(options)
    }

    val programAst = OrcXML.xmlToAst(XML.loadString(programOil))
    val root = new DOrcFollowerExecution(executionId, followerExecutionNum, programAst, options, sendEvent(leaderLocation, executionId, DOrcExecution.noGroupProxyId), this)
    installHandlers(root)

    programs.put(executionId, root)
    roots.put(new WeakReference(root), ())
  }

  def unloadProgram(executionId: DOrcExecution#ExecutionId) {
    Logger.entering(getClass.getName, "unloadProgram", Seq(executionId))
    val removedExecution = programs.remove(executionId)
    if (!removedExecution.members.isEmpty) {
      Logger.fine(s"Unloaded $executionId with ${removedExecution.members.size} group members still in execution")
    }

    assert(programs.isEmpty()) /* For now */
    if (programs.isEmpty()) {
      stopScheduler()
      super.stop()
    }
  }

}

object FollowerRuntime {

  def main(args: Array[String]) {
    assert(args.length == 1)
    new FollowerRuntime(new InetSocketAddress(args(0).toInt)).listen()
  }

}

class LeaderLocation(val connection: SocketObjectConnection[FollowerRuntime#MsgToFollower, LeaderRuntime#MsgToLeader]) extends Location {
  override def send(message: OrcPeerCmd) = connection.send(message)
}
