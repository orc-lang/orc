//
// DOrcRuntime.scala -- Scala class LeaderRuntime
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

import java.io.EOFException
import java.net.InetSocketAddress

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.ref.WeakReference
import scala.util.control.NonFatal

import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions }
import orc.ast.oil.nameless.Expression
import orc.ast.oil.xml.OrcXML
import orc.error.runtime.ExecutionException
import orc.run.core.Token
import orc.util.{ ConnectionInitiator, LatchingSignal, SocketObjectConnection }

/** Orc runtime engine leading a dOrc cluster.
  *
  * @author jthywiss
  */
class LeaderRuntime() extends DOrcRuntime(0, "dOrc leader") {

  protected val runtimeLocationMap = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[Int, FollowerLocation]())

  protected def followerLocations = runtimeLocationMap.values.filterNot({ _ == here }).asInstanceOf[Iterable[FollowerLocation]]
  protected def followerEntries = runtimeLocationMap.filterNot({ _._2 == here })

  override def locationForRuntimeId(runtimeId: DOrcRuntime#RuntimeId): PeerLocation = runtimeLocationMap(runtimeId)

  override def allLocations = runtimeLocationMap.values.toSet

  protected def connectToFollowers() {
    val followers = Map(1 -> new InetSocketAddress("localhost", 36721), 2 -> new InetSocketAddress("localhost", 36722))

    runtimeLocationMap.put(0, here)
    followers foreach { f => runtimeLocationMap.put(f._1, new FollowerLocation(ConnectionInitiator[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd](f._2))) }

    followerEntries foreach { e => new ReceiveThread(e._1, e._2).start() }
    followerEntries foreach { e =>
      followerEntries foreach { _._2.send(AddPeerCmd(e._1, followers(e._1))) }
    }
  }

  val programs = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcLeaderExecution])

  override def run(programAst: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions) {

    connectToFollowers()

    val programOil = OrcXML.astToXml(programAst).toString
    val thisExecutionId = DOrcExecution.freshExecutionId()

    Logger.fine(s"starting scheduler")
    startScheduler(options: OrcExecutionOptions)

    val root = new DOrcLeaderExecution(thisExecutionId, programAst, options, { e => handleExecutionEvent(thisExecutionId, e); eventHandler(e) }, this)
    programs.put(thisExecutionId, root)

    followerEntries foreach { _._2.send(LoadProgramCmd(thisExecutionId, programOil, options)) }

    installHandlers(root)
    roots.add(root)

    /* Initial program token */
    //root.sendToken(new Token(programAst, root), runtimeLocationMap(1))
    val t = new Token(programAst, root)
    schedule(t)

    Logger.exiting(getClass.getName, "run")
  }

  protected def handleExecutionEvent(executionId: DOrcExecution#ExecutionId, event: OrcEvent) {
    Logger.fine(s"Execution got $event")
    event match {
      case HaltedOrKilledEvent => {
        followerLocations foreach { _.send(UnloadProgramCmd(executionId)) }
        programs.remove(executionId)
        if (programs.isEmpty) stop()
      }
      case _ => { /* Other handlers will handle these other event types */ }
    }
  }

  protected class ReceiveThread(followerRuntimeId: DOrcRuntime#RuntimeId, followerLocation: FollowerLocation)
    extends Thread(s"dOrc leader receiver for $followerRuntimeId @ ${followerLocation.connection.socket}") {
    override def run() {
      try {
        followerLocation.send(DOrcConnectionHeader(runtimeId, followerRuntimeId))
        Logger.info(s"Reading events from ${followerLocation.connection.socket}")
        while (!followerLocation.connection.closed && !followerLocation.connection.socket.isInputShutdown) {
          val msg = try {
            followerLocation.connection.receive()
          } catch {
            case _: EOFException => EOF
          }
          Logger.finest(s"Read ${msg}")
          msg match {
            case DOrcConnectionHeader(sid, rid) => assert(sid == followerRuntimeId && rid == runtimeId)
            case NotifyLeaderCmd(xid, event) => LeaderRuntime.this synchronized {
              programs(xid).notifyOrc(event)
            }
            case HostTokenCmd(xid, movedToken) => programs(xid).hostToken(followerLocation, movedToken)
            case PublishGroupCmd(xid, gmpid, t) => programs(xid).publishInGroup(followerLocation, gmpid, t)
            case KillGroupCmd(xid, gpid) => programs(xid).killGroupProxy(gpid)
            case HaltGroupMemberProxyCmd(xid, gmpid) => programs(xid).haltGroupMemberProxy(gmpid)
            case DiscorporateGroupMemberProxyCmd(xid, gmpid) => programs(xid).discorporateGroupMemberProxy(gmpid)
            case ReadFutureCmd(xid, futureId, readerFollowerNum) => programs(xid).readFuture(futureId, readerFollowerNum)
            case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(futureId, value)
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
        runtimeLocationMap.remove(followerRuntimeId)
        followerEntries foreach { _._2.send(RemovePeerCmd(followerRuntimeId)) }
        programs.values foreach { _.notifyOrc(FollowerConnectionClosedEvent(followerLocation)) }
      }
    }
  }

  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  override def runSynchronous(programAst: Expression, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions) {
    synchronized {
      if (runSyncThread != null) throw new IllegalStateException("runSynchronous on an engine that is already running synchronously")
      runSyncThread = Thread.currentThread()
    }

    val doneSignal = new LatchingSignal()
    def syncAction(event: OrcEvent) {
      event match {
        case FollowerConnectionClosedEvent(_) => { if (followerLocations.isEmpty) doneSignal.signal() }
        case _ => {}
      }
      eventHandler(event)
    }

    try {
      run(programAst, eventHandler, options)

      doneSignal.await()
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
    followerEntries foreach { e =>
      followerEntries foreach { _._2.send(RemovePeerCmd(e._1)) }
    }
    followerEntries foreach { e =>
      runtimeLocationMap.remove(e._1)
      e._2.connection.socket.shutdownOutput()
    }
    super.stop()
  }

  val here = Here

  object Here extends FollowerLocation(null) {
    override def send(message: OrcLeaderToFollowerCmd) = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
  }

}

class FollowerLocation(val connection: SocketObjectConnection[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd]) extends Location[OrcLeaderToFollowerCmd] {
  override def send(message: OrcLeaderToFollowerCmd) = connection.send(message)
}

case class FollowerConnectionClosedEvent(location: FollowerLocation) extends OrcEvent
