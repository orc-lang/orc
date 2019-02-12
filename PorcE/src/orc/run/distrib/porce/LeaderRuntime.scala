//
// LeaderRuntime.scala -- Scala classes LeaderRuntime, FollowerLocation, and FollowerConnectionClosedEvent
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

import java.io.{ EOFException, FileWriter }
import java.net.{ InetAddress, InetSocketAddress, SocketException }
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import scala.collection.JavaConverters.mapAsScalaConcurrentMap
import scala.util.control.NonFatal

import orc.{ HaltedOrKilledEvent, OrcEvent, OrcExecutionOptions, Schedulable }
import orc.error.runtime.ExecutionException
import orc.run.distrib.Logger
import orc.run.distrib.common.{ ClosableConnection, LocationMap, RuntimeConnection, RuntimeConnectionListener }
import orc.util.LatchingSignal

/** Orc runtime engine leading a dOrc cluster.
  *
  * @author jthywiss
  */
class LeaderRuntime() extends DOrcRuntime(new DOrcRuntime.RuntimeId(0L), "dOrc leader") {

  ////////
  // Track locations (Orc runtime engine instances) in our cluster
  ////////

  override val here = Here
  override val hereSet = Set(here)

  object Here extends FollowerLocation(new DOrcRuntime.RuntimeId(0L), null, null) {
    override def send(message: OrcLeaderToFollowerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self: " + message)
    override def sendInContext(execution: DOrcExecution)(message: OrcLeaderToFollowerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self: " + message)
  }

  protected val runtimeLocationRegister = new LocationMap[DOrcRuntime.RuntimeId, FollowerLocation](here)

  override def locationForRuntimeId(runtimeId: DOrcRuntime.RuntimeId): PeerLocation = runtimeLocationRegister(runtimeId)

  override def allLocations: Set[PeerLocation] = runtimeLocationRegister.locationSnapshot.asInstanceOf[Set[PeerLocation]]

  override def leader = here
  override def leaderSet = hereSet

  ////////
  // Connect to new followers
  ////////

  protected var listenerThread: ListenerThread = null

  protected def boundListenAddress = synchronized {
    listenerThread.boundListenAddress
  }

  protected def canonicalListenAddress = synchronized {
    listenerThread.canonicalListenAddress
  }

  protected class ListenerThread(listenAddress: InetSocketAddress)
    extends Thread(s"dOrc leader listener on ${listenAddress}") {

    protected val listener = new RuntimeConnectionListener[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd, DOrcExecution, PeerLocation](listenAddress)
    val boundListenAddress = new InetSocketAddress(listener.serverSocket.getInetAddress, listener.serverSocket.getLocalPort)
    val canonicalListenAddress = {
      val hostname = (if (listener.serverSocket.getInetAddress.isAnyLocalAddress) InetAddress.getLocalHost else listener.serverSocket.getInetAddress).getCanonicalHostName
      new InetSocketAddress(hostname, listener.serverSocket.getLocalPort)
    }

    val orderlyShutdown = new AtomicBoolean(false)

    override def run(): Unit = {
      runtimeLocationRegister.put(runtimeId, here)

      Logger.Connect.info(s"Listening on $boundListenAddress, which we'll advertise as $canonicalListenAddress")
      /* this Thread */ setName(s"dOrc leader listener on $boundListenAddress")

      try {
        while (!listener.serverSocket.isClosed()) {
          val newConn = listener.acceptConnection()
          Logger.Connect.finer(s"accepted ${newConn.socket}")

          new ReceiveThread(newConn).start()
        }
      } catch {
        case se: SocketException if se.getMessage == "Socket closed" => /* Ignore */
        case se: SocketException if se.getMessage == "Connection reset" => /* Ignore */
      } finally {
        if (!orderlyShutdown.get) {
          runtimeLocationRegister.locationSnapshot foreach { follower =>
            follower match {
              case a: ClosableConnection => a.abort()
              case _ => /* Ignore */
            }
            runtimeLocationRegister.remove(follower.runtimeId)
          }
          listener.close()
        }
        Logger.Connect.info(s"Closed $boundListenAddress")
      }
    }

    def shutdown(): Unit = {
      orderlyShutdown.set(true)
      listener.close()
    }

  }

  protected class ReceiveThread(connection: RuntimeConnection[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd, DOrcExecution, PeerLocation])
    extends Thread(f"dOrc leader connection with ${connection.socket}") {

    override def run(): Unit = {
      Logger.Connect.entering(getClass.getName, "run", Seq(connection))
      try {
        connection.receive() match {
          case DOrcConnectionHeader(sid, rid, listenSockAddr) if (rid == runtimeId) => {
            setName(f"dOrc leader receiver for follower ${sid.longValue}%#x @ $listenSockAddr")
            val newFollowerLoc = new FollowerLocation(sid, connection, listenSockAddr)

            connection.send(DOrcConnectionHeader(runtimeId, sid, canonicalListenAddress))

            val oldMappedValue = runtimeLocationRegister.put(sid, newFollowerLoc)
            assert(oldMappedValue == None, f"Received DOrcConnectionHeader for follower ${sid.longValue}%#x, but runtimeLocationMap already had ${oldMappedValue.get} for location ${sid.longValue}%#x")

            runtimeLocationRegister.otherLocationsSnapshot foreach { follower =>
              if (follower.runtimeId != sid) {
                /* Let new follower know of existing peer */
                connection.send(AddPeerCmd(follower.runtimeId, follower.listenAddress))
                /* Let old follower know of new peer */
                follower.send(AddPeerCmd(newFollowerLoc.runtimeId, newFollowerLoc.listenAddress))
              }
            }

            communicateWithFollower(newFollowerLoc)
          }
          case DOrcConnectionHeader(sid, rid, _) => throw new AssertionError(f"Received DOrcConnectionHeader with wrong runtime ids: sender=${sid.longValue}%#x, receiver=${rid.longValue}%#x")
          case m => throw new AssertionError(s"Received message before DOrcConnectionHeader: $m")
        }
      } catch {
        case e: Throwable => {
          Logger.Connect.log(Level.SEVERE, "ReceiveThread caught", e);
          throw e
        }
      } finally {
        Logger.Connect.fine(s"Stopped reading events from ${connection.socket}")
        if (!connection.closed) {
          try {
            Logger.Connect.fine(s"ReceiveThread terminating; aborting $connection")
            connection.abort()
          } catch {
            case NonFatal(e2) => Logger.Connect.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
        }
      }
      Logger.Connect.exiting(getClass.getName, "run")
    }
  }

  ////////
  // Communicate with cluster members
  ////////

  protected def communicateWithFollower(followerLocation: FollowerLocation): Unit = {
    try {
      Logger.Connect.fine(s"Reading events from ${followerLocation.connection.socket}")
      while (!followerLocation.connection.closed && !followerLocation.connection.socket.isInputShutdown) {
        val msg = try {
          followerLocation.connection.receiveInContext({ programs(_) }, followerLocation)()
        } catch {
          case _: EOFException => EOF
        }
        //Logger.Message.finest(s"Read ${msg}")
        msg match {
          case ProgramReadyCmd(xid) => programs(xid).followerReady(followerLocation.runtimeId)
          case NotifyLeaderCmd(xid, event) => LeaderRuntime.this synchronized {
            programs(xid).notifyOrc(event)
          }
          case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(followerLocation, gmpid, movedCall)
          case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(followerLocation, gmpid, pub)
          case KilledGroupCmd(xid, gpid) => programs(xid).killedGroupProxy(gpid)
          case KillingGroupCmd(xid, gpid, killing) => programs(xid).killingGroupProxy(followerLocation, gpid, killing)
          case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
          case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
          case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid, followerLocation)
          case ProvideCounterCreditCmd(xid, counterId, credits) => programs(xid).provideCounterCredit(counterId, followerLocation, credits)
          case ReadFutureCmd(xid, futureId, readerFollowerRuntimeId) => programs(xid).readFuture(futureId, readerFollowerRuntimeId)
          case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(followerLocation, futureId, value)
          case ResolveFutureCmd(xid, futureId, value) => programs(xid).receiveFutureResolution(followerLocation, futureId, value)
          case EOF => { Logger.Message.fine(s"EOF, aborting $followerLocation"); followerLocation.connection.abort() }
          case connHdr: DOrcConnectionHeader => throw new AssertionError(s"Received extraneous $connHdr")
        }
      }
    } finally {
      try {
        if (!followerLocation.connection.closed) { Logger.Connect.fine(s"ReceiveThread finally: Closing $followerLocation"); followerLocation.connection.close() }
      } catch {
        case NonFatal(e) => Logger.Connect.finer(s"Ignoring $e") /* Ignore close failures at this point */
      }
      Logger.Connect.fine(s"Stopped reading events from ${followerLocation.connection.socket}")
      runtimeLocationRegister.remove(followerLocation.runtimeId)
      runtimeLocationRegister.otherLocationsSnapshot foreach { follower =>
        try {
          follower.send(RemovePeerCmd(followerLocation.runtimeId))
        } catch {
          case NonFatal(e) => Logger.Connect.finer(s"Ignoring $e") /* Ignore send RemovePeerCmd failures at this point */
        }
      }
      programs.values foreach { _.notifyOrc(FollowerConnectionClosedEvent(followerLocation)) }
    }
  }

  ////////
  // Run & stop programs
  ////////

  val programs = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcLeaderExecution])

  /*override*/ def run(programAst: DOrcRuntime#ProgramAST, eventHandler: OrcEvent => Unit, options: OrcExecutionOptions): Unit = {
    /* TODO: run's programAst type needs to be parameterized */

    synchronized {
      listenerThread = new ListenerThread(options.listenSocketAddress)
      listenerThread.start()
    }

    options.listenSockAddrFile match {
      case Some(f) =>
        val fw = new FileWriter(f)
        try {
          fw.write(canonicalListenAddress.getHostName)
          fw.write(':')
          fw.write(canonicalListenAddress.getPort.toString)
          fw.write('\n')
        } finally {
          fw.close()
        }
      case None =>
    }

    awaitFollowers(options.followerCount)

    val thisExecutionId = DOrcExecution.freshExecutionId()

    Logger.Downcall.fine(s"starting scheduler")
    startScheduler(options: OrcExecutionOptions)

    val leaderExecution = new DOrcLeaderExecution(thisExecutionId, programAst, options, { e => handleExecutionEvent(thisExecutionId, e); eventHandler(e) }, this)

    programs.put(thisExecutionId, leaderExecution)

    val rootCounterId = leaderExecution.getDistributedCounterForCounter(leaderExecution.c).id

    runtimeLocationRegister.otherLocationsSnapshot foreach { follower =>
      leaderExecution.followerStarting(follower.runtimeId)
      schedule(new Schedulable {
        override def run(): Unit = {
          follower.send(LoadProgramCmd(thisExecutionId, programAst, options, rootCounterId))
        }
      })
    }

    installHandlers(leaderExecution)
    addRoot(leaderExecution)

    leaderExecution.awaitAllFollowersReady()

    Logger.info(s"Start run $thisExecutionId")

    leaderExecution.runProgram()

    Logger.exiting(getClass.getName, "run")
  }

  def awaitFollowers(count: Int): Unit = {
    /* runtimeLocationMap.size includes leader */
    while (runtimeLocationRegister.size <= count) {
      runtimeLocationRegister synchronized runtimeLocationRegister.wait()
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
        case FollowerConnectionClosedEvent(_) => { if (runtimeLocationRegister.otherLocationsSnapshot.isEmpty) doneSignal.signal() }
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
    val doShutdown = synchronized {
      if (listenerThread != null) {
        listenerThread.shutdown()
        listenerThread = null
        true
      } else {
        false
      }
    }

    if (doShutdown) {

      val followers = runtimeLocationRegister.otherLocationsSnapshot

      followers foreach { subjectFollower =>
        followers foreach { recipientFollower =>
          try {
            recipientFollower.send(RemovePeerCmd(subjectFollower.runtimeId))
          } catch {
            case NonFatal(e) => Logger.finer(s"Ignoring $e") /* Ignore send RemovePeerCmd failures at this point */
          }
        }
      }

      followers foreach { follower =>
        runtimeLocationRegister.remove(follower.runtimeId)
        try {
          follower.connection.socket.shutdownOutput()
        } catch {
          case NonFatal(e) => Logger.Connect.finer(s"Ignoring $e") /* Ignore shutdownOutput failures at this point */
        }
      }

    }

    super.stop()
  }

  ////////
  // Propagate Orc events to the handlers/environment
  ////////

  protected def handleExecutionEvent(executionId: DOrcExecution#ExecutionId, event: OrcEvent): Unit = {
    Logger.fine(s"Execution got $event")
    event match {
      case HaltedOrKilledEvent => {

        Logger.info(s"Stop run $executionId")

        runtimeLocationRegister.otherLocationsSnapshot foreach { _.send(UnloadProgramCmd(executionId)) }
        programs.remove(executionId)
        if (programs.isEmpty) stop()
      }
      case _ => { /* Other handlers will handle these other event types */ }
    }
  }

}

class FollowerLocation(val runtimeId: DOrcRuntime.RuntimeId, val connection: RuntimeConnection[OrcFollowerToLeaderCmd, OrcLeaderToFollowerCmd, DOrcExecution, PeerLocation], val listenAddress: InetSocketAddress) extends RuntimeRef[OrcLeaderToFollowerCmd] {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId, listenAddress=$listenAddress)"

  override def send(message: OrcLeaderToFollowerCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcLeaderToFollowerCmd): Unit = connection.sendInContext(execution, this)(message)
}

case class FollowerConnectionClosedEvent(location: FollowerLocation) extends OrcEvent
