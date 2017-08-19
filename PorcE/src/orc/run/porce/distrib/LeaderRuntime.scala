//
// LeaderRuntime.scala -- Scala classes LeaderRuntime, FollowerLocation, and FollowerConnectionClosedEvent
// Project PorcE
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import java.io.EOFException
import java.net.InetSocketAddress

import scala.collection.JavaConverters.{ asScalaBufferConverter, mapAsScalaConcurrentMap }
import scala.util.control.NonFatal

import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions }
import orc.error.runtime.ExecutionException
import orc.util.LatchingSignal

/** Orc runtime engine leading a dOrc cluster.
  *
  * @author jthywiss
  */
class LeaderRuntime() extends DOrcRuntime(0, "dOrc leader") {

  protected val runtimeLocationMap: scala.collection.mutable.Map[Int, FollowerLocation] =
    mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcRuntime#RuntimeId, FollowerLocation]())

  protected def followerLocations: Iterable[FollowerLocation] = runtimeLocationMap.values.filterNot({ _ == here }).asInstanceOf[Iterable[FollowerLocation]]
  protected def followerEntries: scala.collection.mutable.Map[Int, FollowerLocation] = runtimeLocationMap.filterNot({ _._2 == here })

  override def locationForRuntimeId(runtimeId: DOrcRuntime#RuntimeId): PeerLocation = runtimeLocationMap(runtimeId)

  override def allLocations: Set[PeerLocation] = runtimeLocationMap.values.toSet

  protected def connectToFollowers(followers: Map[Int, InetSocketAddress]): Unit = {
    runtimeLocationMap.put(0, here)
    followers foreach { f => runtimeLocationMap.put(f._1, new FollowerLocation(f._1, RuntimeConnectionInitiator[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd](f._2))) }

    followerEntries foreach { e => 
      e._2.send(DOrcConnectionHeader(runtimeId, e._1))
      new ReceiveThread(e._1, e._2).start()
    }
    followerEntries foreach { e =>
      followerEntries foreach { _._2.send(AddPeerCmd(e._1, followers(e._1))) }
    }
  }

  val programs = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcLeaderExecution])

  /*override*/ def run(programAst: DOrcRuntime#ProgramAST, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions): Unit = {
    val followers = Map(options.followerSockets.asScala.toSeq.zipWithIndex.map({ case (s, i) => (i + 1, s) }): _*)
    connectToFollowers(followers)

    val thisExecutionId = DOrcExecution.freshExecutionId()

    Logger.fine(s"starting scheduler")
    startScheduler(options: OrcExecutionOptions)

    val leaderExecution = new DOrcLeaderExecution(thisExecutionId, programAst, options, { e => handleExecutionEvent(thisExecutionId, e); eventHandler(e) }, this)

    programs.put(thisExecutionId, leaderExecution)
    
    val rootCounterId = leaderExecution.makeProxyWithinCounter(leaderExecution.c)

    followerEntries foreach { f => {
        leaderExecution.followerStarting(f._1)
        f._2.send(LoadProgramCmd(thisExecutionId, programAst, options, rootCounterId)) 
      }
    }

    installHandlers(leaderExecution)
    addRoot(leaderExecution)

    leaderExecution.awaitAllFollowersReady()

    leaderExecution.runProgram()

    Logger.exiting(getClass.getName, "run")
  }

  protected def handleExecutionEvent(executionId: DOrcExecution#ExecutionId, event: OrcEvent): Unit = {
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
    extends Thread(f"dOrc leader receiver for $followerRuntimeId%#x @ ${followerLocation.connection.socket}") {
    override def run(): Unit = {
      try {
        Logger.info(s"Reading events from ${followerLocation.connection.socket}")
        while (!followerLocation.connection.closed && !followerLocation.connection.socket.isInputShutdown) {
          val msg = try {
            followerLocation.connection.receiveInContext({ programs(_) }, followerLocation)()
          } catch {
            case _: EOFException => EOF
          }
          Logger.finest(s"Read ${msg}")
          msg match {
            case DOrcConnectionHeader(sid, rid) => assert(sid == followerRuntimeId && rid == runtimeId)
            case ProgramReadyCmd(xid) => programs(xid).followerReady(followerRuntimeId)
            case NotifyLeaderCmd(xid, event) => LeaderRuntime.this synchronized {
              programs(xid).notifyOrc(event)
            }
            case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(followerLocation, gmpid, movedCall)
            case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(followerLocation, gmpid, pub)
            case KilledGroupCmd(xid, gpid) => programs(xid).killedGroupProxy(gpid)
            case KillingGroupCmd(xid, gpid, killing) => programs(xid).killingGroupProxy(followerLocation, gpid, killing)
            case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
            case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
            case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid)
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
        followerEntries foreach { fe =>
          try {
            fe._2.send(RemovePeerCmd(followerRuntimeId))
          } catch {
            case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore send RemovePeerCmd failures at this point */
          }
        }
        programs.values foreach { _.notifyOrc(FollowerConnectionClosedEvent(followerLocation)) }
      }
    }
  }

  @throws(classOf[ExecutionException])
  @throws(classOf[InterruptedException])
  /*override*/ def runSynchronous(programAst: DOrcRuntime#ProgramAST, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions): Unit = {
    synchronized {
      if (runSyncThread != null) throw new IllegalStateException("runSynchronous on an engine that is already running synchronously")
      runSyncThread = Thread.currentThread()
    }

    val doneSignal = new LatchingSignal()
    def syncAction(event: OrcEvent): Unit = {
      event match {
        case FollowerConnectionClosedEvent(_) => { if (followerLocations.isEmpty) doneSignal.signal() }
        case _ => {}
      }
      eventHandler(event)
    }

    try {
      run(programAst, syncAction(_), options)

      doneSignal.await()
    } finally {
      /* Important: runSyncThread must be null before calling stop */
      synchronized {
        runSyncThread = null
      }
      this.stop()
      Logger.exiting(getClass.getName, "runSynchronous")
    }
  }

  override def stop(): Unit = {
    followerEntries foreach { subjectFE =>
      followerEntries foreach { recipientFE =>
        try {
          recipientFE._2.send(RemovePeerCmd(subjectFE._1))
        } catch {
          case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore send RemovePeerCmd failures at this point */
        }
      }
    }
    followerEntries foreach { e =>
      runtimeLocationMap.remove(e._1)
      try {
        e._2.connection.socket.shutdownOutput()
      } catch {
        case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore shutdownOutput failures at this point */
      }
    }
    super.stop()
  }

  val here = Here

  object Here extends FollowerLocation(0, null) {
    override def send(message: OrcLeaderToFollowerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
    override def sendInContext(execution: DOrcExecution)(message: OrcLeaderToFollowerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
  }

}

class FollowerLocation(val runtimeId: DOrcRuntime#RuntimeId, val connection: RuntimeConnection[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd]) extends Location[OrcLeaderToFollowerCmd] {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"

  override def send(message: OrcLeaderToFollowerCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcLeaderToFollowerCmd): Unit = connection.sendInContext(execution, this)(message)
}

case class FollowerConnectionClosedEvent(location: FollowerLocation) extends OrcEvent
