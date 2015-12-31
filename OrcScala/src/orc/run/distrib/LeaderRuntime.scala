//
// DOrcRuntime.scala -- Scala class LeaderRuntime
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

import java.io.EOFException
import java.net.InetSocketAddress

import scala.ref.WeakReference
import scala.util.control.NonFatal

import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions }
import orc.ast.oil.nameless.Expression
import orc.ast.oil.xml.OrcXML
import orc.error.runtime.ExecutionException
import orc.run.core.Token
import orc.util.ConnectionInitiator

/** Orc runtime engine leading a dOrc cluster.
  *
  * @author jthywiss
  */
class LeaderRuntime() extends DOrcRuntime("dOrc leader") {

  type MsgToLeader = OrcFollowerToLeaderCmd

  val followerLocations = scala.collection.mutable.Set.empty[FollowerLocation]

  override def run(programAst: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    val programOil = OrcXML.astToXml(programAst).toString()
    val thisRunId = DOrcExecution.freshExecutionId()
    val followers = Set(new InetSocketAddress("localhost", 36721), new InetSocketAddress("localhost", 36722))
    followers foreach { f => followerLocations.add(new FollowerLocation(f)) }

    Logger.fine(s"starting scheduler")
    startScheduler(options: OrcExecutionOptions)

    val root = new DOrcLeaderExecution(thisRunId, programAst, options, { e => handleExecutionEvent(thisRunId, e); k(e) }, this)

    followerLocations foreach { l => (new ReceiveThread(root, l, k)).start() }

    var flwrNum = 0
    followerLocations foreach { _.connection.send(LoadProgramCmd(thisRunId, { flwrNum += 1; flwrNum }, programOil, options)) }

    installHandlers(root)
    roots.put(new WeakReference(root), ())

    /* Initial program token */
    root.sendToken(new Token(programAst, root), followerLocations.head)

    Logger.exiting(getClass.getName, "run")
  }

  protected def handleExecutionEvent(executionId: DOrcExecution#ExecutionId, event: OrcEvent) {
    Logger.fine(s"Execution got $event")
    event match {
      case HaltedOrKilledEvent => {
        followerLocations foreach { _.connection.send(UnloadProgramCmd(executionId)) }
      }
      case _ => { /* Other handlers will handle these other event types */ }
    }
  }

  protected class ReceiveThread(execution: DOrcLeaderExecution, followerLocation: FollowerLocation, k: OrcEvent => Unit)
    extends Thread(s"dOrc leader receiver for ${followerLocation.connection.socket}") {
    override def run() {
      try {
        Logger.info(s"Reading events from ${followerLocation.connection.socket}")
        while (!followerLocation.connection.closed && !followerLocation.connection.socket.isInputShutdown) {
          val msg = try {
            followerLocation.connection.receive()
          } catch {
            case _: EOFException => EOF
          }
          Logger.finest(s"Read ${msg}")
          msg match {
            case NotifyLeaderCmd(xid, event) => LeaderRuntime.this synchronized {
              assert(xid == execution.executionId)
              execution.notifyOrc(event)
            }
            case HostTokenCmd(xid, movedToken) => { assert(xid == execution.executionId); execution.hostToken(followerLocation, movedToken) }
            case PublishGroupCmd(xid, gmpid, t, v) => { assert(xid == execution.executionId); execution.publishInGroup(gmpid, t, v) }
            case KillGroupCmd(xid, gpid) => { assert(xid == execution.executionId); execution.killGroupProxy(gpid) }
            case HaltGroupMemberProxyCmd(xid, gmpid) => { assert(xid == execution.executionId); execution.haltGroupMemberProxy(gmpid) }
            case EOF => { Logger.fine(s"EOF, aborting $followerLocation"); followerLocation.connection.abort() }
          }
        }
      } finally {
        try {
          if (!followerLocation.connection.closed) { Logger.fine(s"ReceiveThread finally: Closing $followerLocation"); followerLocation.connection.close() }
        } catch {
          case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore close failures at this point */
        }
        Logger.info(s"Stopped reading events from ${followerLocation.connection.socket}")
        followerLocations synchronized { !followerLocations.remove(followerLocation) }
      }
    }
  }

  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  override def runSynchronous(programAst: Expression, k: OrcEvent => Unit, options: OrcExecutionOptions) {
    synchronized {
      if (runSyncThread != null) throw new IllegalStateException("runSynchronous on an engine that is already running synchronously")
      runSyncThread = Thread.currentThread()
    }

    try {
      run(programAst, k, options)

      //FIXME:Don't busy wait
      while (followerLocations synchronized { !followerLocations.isEmpty }) {
        Thread.sleep(150)
      }
      Thread.sleep(150)
    } finally {
      // Important: runSyncThread must be null before calling stop
      synchronized {
        runSyncThread = null
      }
      this.stop()
      Logger.exiting(getClass.getName, "runSynchronous")
    }
  }

  override def stop() = {
    followerLocations foreach { _.connection.socket.shutdownOutput() }
    followerLocations.clear()
    super.stop()
  }

  override def here: Location = Here
  override def currentLocations(v: Any) = Set(Here) //followerLocations.toSet + here
  override def permittedLocations(v: Any) = followerLocations.toSet + here

  object Here extends Location {
    def send(message: OrcPeerCmd) = ???
  }
}

class FollowerLocation(remoteSockAddr: InetSocketAddress) extends Location {
  val connection = ConnectionInitiator[LeaderRuntime#MsgToLeader, FollowerRuntime#MsgToFollower](remoteSockAddr)
  override def send(message: OrcPeerCmd) = connection.send(message)
}
