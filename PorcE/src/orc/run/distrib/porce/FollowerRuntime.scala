//
// FollowerRuntime.scala -- Scala classes FollowerRuntime, LeaderLocation, and PeerLocationImpl
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

import java.io.{ EOFException, File, FileWriter, IOException, ObjectOutputStream }
import java.net.{ InetAddress, InetSocketAddress, SocketException }
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import scala.collection.JavaConverters.mapAsScalaConcurrentMap
import scala.util.control.NonFatal

import orc.{ CaughtEvent, OrcEvent, OrcExecutionOptions }
import orc.run.distrib.Logger
import orc.run.distrib.common.{ ClosableConnection, FollowerRuntimeCmdLineOptions, LocationMap, RuntimeConnection, RuntimeConnectionInitiator, RuntimeConnectionListener }
import orc.util.{ CmdLineUsageException, ExitStatus, MainExit, PrintVersionAndMessageException }

/** Orc runtime engine running as part of a dOrc cluster.
  *
  * @author jthywiss
  */
class FollowerRuntime(runtimeId: DOrcRuntime.RuntimeId) extends DOrcRuntime(runtimeId, s"dOrc follower $runtimeId") {

  ////////
  // Track locations (Orc runtime engine instances) in our cluster
  ////////

  override val here = Here
  override val hereSet = Set(here)

  object Here extends PeerLocation {
    override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"
    override def send(message: OrcPeerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self: " + message)
    override def sendInContext(execution: DOrcExecution)(message: OrcPeerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self: " + message)
    override def runtimeId: DOrcRuntime.RuntimeId = FollowerRuntime.this.runtimeId
  }

  protected val runtimeLocationRegister = new LocationMap[DOrcRuntime.RuntimeId, PeerLocation](here)

  override def locationForRuntimeId(runtimeId: DOrcRuntime.RuntimeId): PeerLocation = runtimeLocationRegister(runtimeId)

  override def allLocations: Set[PeerLocation] = runtimeLocationRegister.locationSnapshot

  override def leader = locationForRuntimeId(new DOrcRuntime.RuntimeId(0L))
  override def leaderSet = Set(leader)

  ////////
  // Listen on our listen address; also connect to leader
  ////////

  protected var listener: RuntimeConnectionListener[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd, DOrcExecution, PeerLocation] = null
  protected var boundListenAddress: InetSocketAddress = null
  protected var canonicalListenAddress: InetSocketAddress = null

  val orderlyShutdown = new AtomicBoolean(false)

  def listenAndContactLeader(listenAddress: InetSocketAddress, leaderAddress: InetSocketAddress, listenSockAddrFile: Option[File]): Unit = {
    runtimeLocationRegister.put(runtimeId, here)

    listener = new RuntimeConnectionListener[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd, DOrcExecution, PeerLocation](listenAddress)
    boundListenAddress = new InetSocketAddress(listener.serverSocket.getInetAddress, listener.serverSocket.getLocalPort)
    canonicalListenAddress = {
      val hostname = (if (listener.serverSocket.getInetAddress.isAnyLocalAddress) InetAddress.getLocalHost else listener.serverSocket.getInetAddress).getCanonicalHostName
      new InetSocketAddress(hostname, listener.serverSocket.getLocalPort)
    }

    Logger.Connect.info(s"Listening on $boundListenAddress, which we'll advertise as $canonicalListenAddress")

    listenSockAddrFile match {
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

    contactLeader(leaderAddress)

    try {
      while (!listener.serverSocket.isClosed()) {
        val newConn = listener.acceptConnection()
        Logger.Connect.finer(s"accepted ${newConn.socket}")

        MessageProcessorThread.accept(newConn)

      }
    } catch {
      case se: SocketException if se.getMessage == "Socket closed" => /* Ignore */
      case se: SocketException if se.getMessage == "Connection reset" => /* Ignore */
    } finally {
      if (!orderlyShutdown.get()) {
        runtimeLocationRegister.locationSnapshot foreach { location =>
          location match {
            case a: ClosableConnection => a.abort()
            case _ => /* Ignore */
          }
          runtimeLocationRegister.remove(location.runtimeId)
        }
        listener.close()
      }
      listener = null
      Logger.Connect.info(s"Closed $boundListenAddress")
      boundListenAddress = null
    }
  }

  protected def contactLeader(leaderAddress: InetSocketAddress): Unit = {
    MessageProcessorThread.initiateToLeader(leaderAddress)
  }

  protected def followLeader(leaderLocation: LeaderLocation): Unit = {
    Logger.Connect.entering(getClass.getName, "followLeader")
    try {
      var done = false

      Logger.Message.fine(s"Reading events from ${leaderLocation.connection.socket}")

      while (!done && !leaderLocation.connection.closed && !leaderLocation.connection.socket.isInputShutdown) {
        val cmd = try {
          leaderLocation.connection.receiveInContext({ programs(_) }, leaderLocation)
        } catch {
          case _: EOFException => EOF
        }
        //Logger.Message.finest(s"received $cmd")
        cmd match {
          case AddPeerCmd(peerRuntimeId, peerListenAddress) => addPeer(peerRuntimeId, peerListenAddress)
          case RemovePeerCmd(peerRuntimeId) => removePeer(peerRuntimeId)
          case LoadProgramCmd(xid, oil, options, rootCounterId) => loadProgram(leaderLocation, xid, oil, options, rootCounterId)
          case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(leaderLocation, gmpid, movedCall)
          case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(leaderLocation, gmpid, pub)
          case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
          case KilledGroupCmd(xid, gpid) => programs(xid).killedGroupProxy(gpid)
          case KillingGroupCmd(xid, gpid, killing) => programs(xid).killingGroupProxy(leaderLocation, gpid, killing)
          case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
          case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid, leaderLocation)
          case ProvideCounterCreditCmd(xid, counterId, credits) => programs(xid).provideCounterCredit(counterId, leaderLocation, credits)
          case ReadFutureCmd(xid, futureId, readerFollowerRuntimeId) => programs(xid).readFuture(futureId, readerFollowerRuntimeId)
          case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(leaderLocation, futureId, value)
          case ResolveFutureCmd(xid, futureId, value) => programs(xid).receiveFutureResolution(leaderLocation, futureId, value)
          case UnloadProgramCmd(xid) => unloadProgram(xid)
          case EOF => done = true
          case connHdr: DOrcConnectionHeader => throw new AssertionError(s"Received extraneous $connHdr")
        }
      }
    } finally {
      if (!programs.isEmpty) Logger.ProgLoad.warning(s"Shutting down with ${programs.size} programs still loaded: ${programs.keys.mkString(",")}")
      programs.clear()
      stopScheduler()
      runtimeLocationRegister.remove(new DOrcRuntime.RuntimeId(0L))
      FollowerRuntime.this.stop()
    }
    Logger.Connect.finer(s"runtimeLocationRegister.size = ${runtimeLocationRegister.size}; runtimeLocationRegister = $runtimeLocationRegister")
    Logger.Connect.exiting(getClass.getName, "followLeader")
  }

  ////////
  // Communicate with cluster peers
  ////////

  private val initiatedPeers = scala.collection.mutable.Set[DOrcRuntime.RuntimeId]()

  protected def communicateWithPeer(peerLocation: PeerLocationImpl): Unit = {
    try {
      Logger.Connect.fine(s"Reading events from ${peerLocation.connection.socket}")
      while (!peerLocation.connection.closed && !peerLocation.connection.socket.isInputShutdown) {
        val msg = try {
          peerLocation.connection.receiveInContext({ programs(_) }, peerLocation)
        } catch {
          case _: EOFException => EOF
        }
        //Logger.Message.finest(s"Read ${msg}")
        msg match {
          case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(peerLocation, gmpid, movedCall)
          case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(peerLocation, gmpid, pub)
          case KilledGroupCmd(xid, gpid) => programs(xid).killedGroupProxy(gpid)
          case KillingGroupCmd(xid, gpid, killing) => programs(xid).killingGroupProxy(peerLocation, gpid, killing)
          case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
          case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
          case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid, peerLocation)
          case ProvideCounterCreditCmd(xid, counterId, credits) => programs(xid).provideCounterCredit(counterId, peerLocation, credits)
          case ReadFutureCmd(xid, futureId, readerFollowerRuntimeId) => programs(xid).readFuture(futureId, readerFollowerRuntimeId)
          case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(peerLocation, futureId, value)
          case ResolveFutureCmd(xid, futureId, value) => programs(xid).receiveFutureResolution(peerLocation, futureId, value)
          case EOF => { Logger.Message.fine(s"EOF, aborting peerLocation"); peerLocation.connection.abort() }
          case connHdr: DOrcConnectionHeader => throw new AssertionError(s"Received extraneous $connHdr")
        }
      }
    } finally {
      runtimeLocationRegister.remove(peerLocation.runtimeId)
    }
  }

  def addPeer(peerRuntimeId: DOrcRuntime.RuntimeId, peerListenAddress: InetSocketAddress): Unit = {
    Logger.Connect.entering(getClass.getName, "addPeer", Seq(peerRuntimeId.toString, peerListenAddress))
    if (peerListenAddress == boundListenAddress || peerRuntimeId == runtimeId) {
      /* Hey, that's me! */
      assert(peerRuntimeId == runtimeId, s"addPeer for self, but runtimeId was $peerRuntimeId instead of $runtimeId")
      assert(runtimeLocationRegister(peerRuntimeId) == here, s"addPeer for self, but runtimeLocationRegister != here")
    } else {
      if (shouldOpen(peerListenAddress)) {
        val haventInitedYet = initiatedPeers synchronized initiatedPeers.add(peerRuntimeId)
        if (haventInitedYet) {
          Logger.Connect.finest(s"Add peer $peerRuntimeId; Will initiate connection")
          MessageProcessorThread.initiateToPeer(peerRuntimeId, peerListenAddress)
        } else {
          Logger.Connect.finest(s"Add peer $peerRuntimeId; Already initiated connection")
        }
      } else {
        Logger.Connect.finest(s"Add peer $peerRuntimeId; Expecting peer to initiate connection (it may have already)")
      }
    }
    Logger.Connect.exiting(getClass.getName, "addPeer")
  }

  protected def shouldOpen(peerListenAddress: InetSocketAddress): Boolean = {
    /* Convention: Peer with "lower" address initiates connection */
    val cla = (canonicalListenAddress.getAddress.getAddress map { _.toInt }) :+ canonicalListenAddress.getPort
    val pla = (peerListenAddress.getAddress.getAddress map { _.toInt }) :+ peerListenAddress.getPort
    scala.math.Ordering.Iterable[Int].lt(cla, pla)
  }

  def removePeer(peerRuntimeId: DOrcRuntime.RuntimeId): Unit = {
    Logger.Connect.entering(getClass.getName, "removePeer", Seq(peerRuntimeId.toString))
    if (peerRuntimeId == runtimeId) {
      /* If the leader sends RemovePeer with our ID, we shutdown */
      Logger.Connect.fine("Closing listen socket")
      orderlyShutdown.set(true)
      listener.close()
    } else {
      runtimeLocationRegister.remove(peerRuntimeId) match {
        case Some(removedLocation: LeaderLocation) => /* leave leader connection open */
        case Some(removedLocation: ClosableConnection) => removedLocation.abort()
        case _ => /* Nothing to do */
      }
    }
  }

  ////////
  // MessageProcessorThreads used for our connections with other cluster members
  ////////

  protected abstract class MessageProcessorThread[R <: OrcCmd, S <: OrcCmd](name: String)
    extends Thread(name) {

    override def run(): Unit = {
      Logger.Connect.entering(getClass.getName, "run")

      val connection = getOrOpenConnection

      try {
        sendConnectionHeaderBeforePeer(connection)

        val (newPeerLocation, newPeerListenAddress) = receiveConnectionHeader(connection)

        sendConnectionHeaderAfterPeer(connection, newPeerLocation.runtimeId)

        addPeerToLocationMap(newPeerLocation, newPeerListenAddress)

        newPeerLocation match {
          case leaderLocation: LeaderLocation => followLeader(leaderLocation)
          case newPeerLoc: PeerLocationImpl => communicateWithPeer(newPeerLoc)
        }
      } catch {
        case e: Throwable => {
          Logger.Connect.log(Level.SEVERE, "MessageProcessorThread caught", e);
          throw e
        }
      } finally {
        Logger.Connect.fine(s"Stopped reading events from ${connection.socket}")
        if (!connection.closed) {
          try {
            Logger.Connect.fine(s"MessageProcessorThread terminating; aborting $connection")
            connection.abort()
          } catch {
            case NonFatal(e2) => Logger.Connect.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
        }
      }
      Logger.Connect.exiting(getClass.getName, "run")
    }

    /** Get the connection that this thread will handle.  Either return an already-open one, or create a fresh one. */
    protected def getOrOpenConnection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]

    /** Opportunity to send connection header before receiving peer's */
    protected def sendConnectionHeaderBeforePeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]): Unit

    protected def receiveConnectionHeader(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]) = {
      connection.receive() match {
        /* Ugly: The ids in the connection header tell us the type of the subsequent commands */
        case hdr @ DOrcConnectionHeader(sid, rid, leaderListenAddress) if (sid.longValue == 0L && checkConnectionHeader(hdr)) => {
          setName(s"dOrc MessageProcessor for leader @ ${connection.socket}")

          val newConnAsLeaderConn = connection.asInstanceOf[RuntimeConnection[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd, DOrcExecution, PeerLocation]]
          val leaderLocation = new LeaderLocation(new DOrcRuntime.RuntimeId(0L), newConnAsLeaderConn)
          (leaderLocation, leaderListenAddress)
        }
        case hdr @ DOrcConnectionHeader(sid, rid, peerListenAddress) if (checkConnectionHeader(hdr)) => {
          setName(s"dOrc MessageProcessor for peer $sid @ ${connection.socket}")

          val newConnAsPeerConn = connection.asInstanceOf[RuntimeConnection[OrcPeerCmd, OrcPeerCmd, DOrcExecution, PeerLocation]]
          val newPeerLoc = new PeerLocationImpl(sid, newConnAsPeerConn, peerListenAddress)
          (newPeerLoc, peerListenAddress)
        }
        case DOrcConnectionHeader(sid, rid, _) => throw new AssertionError(s"Received DOrcConnectionHeader with wrong runtime ids: sender=$sid, receiver=$rid")
        case m => throw new AssertionError(s"Received message before DOrcConnectionHeader: $m")
      }
    }

    protected def checkConnectionHeader(receivedHeader: DOrcConnectionHeader) = {
      receivedHeader.receivingRuntimeId == runtimeId
    }

    /** Add newPeerLoc to runtimeLocationRegister if absent. Ignore new value if in map, but identical.  Throw is inconsistent new value. */
    protected def addPeerToLocationMap(newPeerLoc: PeerLocation, newPeerListenAddress: InetSocketAddress): PeerLocation = {
      /* We can have redundant adds of non-leader peers, but the leader can only be added once. */
      runtimeLocationRegister.putIfAbsent(newPeerLoc.runtimeId, newPeerLoc) match {
        case Some(pl: PeerLocationImpl) if (pl.connection.socket.getRemoteSocketAddress == newPeerListenAddress) => pl /* We already know this peer */
        case Some(pl) => throw new AssertionError(s"Received DOrcConnectionHeader for peer $newPeerLoc, but runtimeLocationRegister already had $pl")
        case None => newPeerLoc /* New peer */
      }
    }

    /** Opportunity to send connection header after receiving peer's */
    protected def sendConnectionHeaderAfterPeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation], peerRuntimeId: DOrcRuntime.RuntimeId): Unit

    protected def sendConnectionHeader(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation], peerRuntimeId: DOrcRuntime.RuntimeId) = {
      connection.send(DOrcConnectionHeader(runtimeId, peerRuntimeId, canonicalListenAddress).asInstanceOf[S])
    }

  }

  protected object MessageProcessorThread {

    def initiateToLeader(leaderListenAddress: InetSocketAddress) = new MessageProcessorIThread(new DOrcRuntime.RuntimeId(0L), leaderListenAddress).start()

    def initiateToPeer(peerRuntimeId: DOrcRuntime.RuntimeId, peerListenAddress: InetSocketAddress) = new MessageProcessorIThread(peerRuntimeId, peerListenAddress).start()

    def accept[R <: OrcCmd, S <: OrcCmd](connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]) = new MessageProcessorAThread[R, S](connection).start()

  }

  protected class MessageProcessorAThread[R <: OrcCmd, S <: OrcCmd](connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation])
    extends MessageProcessorThread[R, S](s"dOrc MessageProcessor for unidentified peer @ ${connection.socket} [ACCEPTING]") {

    override protected def getOrOpenConnection = connection

    override protected def sendConnectionHeaderBeforePeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]) = {}

    override protected def sendConnectionHeaderAfterPeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation], peerRuntimeId: DOrcRuntime.RuntimeId) = sendConnectionHeader(connection, peerRuntimeId)

  }

  protected class MessageProcessorIThread[R <: OrcCmd, S <: OrcCmd](peerRuntimeId: DOrcRuntime.RuntimeId, peerListenAddress: InetSocketAddress)
    extends MessageProcessorThread[R, S](s"dOrc MessageProcessor for peer $peerRuntimeId @ $peerListenAddress [INITIATING]") {

    override protected lazy val getOrOpenConnection = RuntimeConnectionInitiator[R, S, DOrcExecution, PeerLocation](peerListenAddress)

    override protected def sendConnectionHeaderBeforePeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation]) = sendConnectionHeader(connection, peerRuntimeId)

    protected override def checkConnectionHeader(receivedHeader: DOrcConnectionHeader) = {
      super.checkConnectionHeader(receivedHeader) && receivedHeader.sendingRuntimeId == peerRuntimeId
    }

    override protected def sendConnectionHeaderAfterPeer(connection: RuntimeConnection[R, S, DOrcExecution, PeerLocation], peerRuntimeId: DOrcRuntime.RuntimeId) = {}

  }

  ////////
  // Load & unload programs
  ////////

  val programs = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcFollowerExecution])

  def loadProgram(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId, programAst: DOrcRuntime#ProgramAST, options: OrcExecutionOptions, rootCounterId: CounterProxyManager#DistributedCounterId): Unit = {
    Logger.ProgLoad.entering(getClass.getName, "loadProgram")

    assert(programs.isEmpty, "loadProgram with other program(s) loaded") /* For now */
    if (programs.isEmpty) {
      Logger.Downcall.fine("starting scheduler")
      startScheduler(options)
    }

    /* For now, runtime IDs and Execution follower numbers are the same.  When
     * we host more than one execution in an engine, they will be different. */
    val followerExecution =
      new DOrcFollowerExecution(executionId, runtimeId.longValue.toInt, programAst, options,
        sendEvent(leaderLocation, executionId),
        rootCounterId, leaderLocation, this)
    installHandlers(followerExecution)

    programs.put(executionId, followerExecution)
    addRoot(followerExecution)

    leaderLocation.sendInContext(followerExecution)(ProgramReadyCmd(executionId))
  }

  def unloadProgram(executionId: DOrcExecution#ExecutionId): Unit = {
    Logger.ProgLoad.entering(getClass.getName, "unloadProgram", Seq(executionId))
    programs.remove(executionId) match {
      case None => Logger.ProgLoad.warning(s"Received unload for unknown (or previously unloaded) execution $executionId")
      case Some(removedExecution) =>
      // FIXME: Figure out how to implement this warning. Currently there is no way to tell if a follower execution is completed since it doesn't have a single root counter.
      //if (!removedExecution.isDone) {
      //  Logger.ProgLoad.fine(s"Unloaded $executionId with group members still in execution")
      //}
    }

    assert(programs.isEmpty, "unloadProgram left program(s) loaded") /* For now */
    if (programs.isEmpty) {
      stopScheduler()
      super.stop()
    }
  }

  ////////
  // Propagate Orc events to the leader
  ////////

  def sendEvent(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId)(event: OrcEvent): Unit = {
    def exceptionWhileMarshaling(throwableUnderSuspicion: Throwable) = {
      try {
        val nullOos = new ObjectOutputStream(new java.io.OutputStream() { override def write(b: Int): Unit = {} })
        nullOos.writeObject(throwableUnderSuspicion)
        None
      } catch {
        case NonFatal(e) => Some(e)
      }
    }

    Logger.entering(getClass.getName, "sendEvent", Seq(leaderLocation, executionId, event))
    val execution = programs(executionId)

    val eventWithoutBadThrowable = event match {
      case CaughtEvent(e) => {
        exceptionWhileMarshaling(e) match {
          case None => event
          case Some(nse) => {
            val replacementThrowable = new Throwable("Replacement for corrupt (serialization failed) Throwable: " + orc.util.GetScalaTypeName(e) + ": " + e.getMessage)
            replacementThrowable.setStackTrace(e.getStackTrace)
            replacementThrowable.addSuppressed(nse)
            CaughtEvent(replacementThrowable)
          }
        }
      }
      case _ => event
    }

    try {
      Tracer.traceOrcEventSend(here, leaderLocation)
      leaderLocation.sendInContext(execution)(NotifyLeaderCmd(executionId, eventWithoutBadThrowable))
    } catch {
      case e1: SocketException => {
        if (!leaderLocation.connection.closed) {
          try {
            Logger.Connect.fine(s"SocketException, aborting $leaderLocation")
            leaderLocation.connection.abort()
          } catch {
            case e2: IOException => Logger.Connect.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
          throw e1
        } else {
          Logger.Connect.finer(s"Ignoring $e1") /* Ignore failures on a closed socket */
        }
      }
    }
    Logger.exiting(getClass.getName, "sendEvent")
  }

}

object FollowerRuntime extends MainExit {

  def main(args: Array[String]): Unit = {
    haltOnUncaughtException()
    try {
      Logger.config(orc.Main.orcImplName + " " + orc.Main.orcVersion)
      val frOptions = new FollowerRuntimeCmdLineOptions()
      frOptions.parseCmdLine(args)
      Logger.config("FollowerRuntime options & operands: " + frOptions.composeCmdLine().mkString(" "))

      Logger.Connect.finer("Calling FollowerRuntime.listen")
      /* For now, runtime IDs and Execution follower numbers are the same.  When
       * we host more than one execution in an engine, they will be different. */
      new FollowerRuntime(new DOrcRuntime.RuntimeId(frOptions.runtimeId)).listenAndContactLeader(frOptions.listenSocketAddress, frOptions.leaderSocketAddress, frOptions.listenSockAddrFile)
    } catch {
      case e: CmdLineUsageException => failureExit(e.getMessage, ExitStatus.Usage)
      case e: PrintVersionAndMessageException => println(orc.Main.orcImplName + " " + orc.Main.orcVersion + "\n" + orc.Main.orcURL + "\n" + orc.Main.orcCopyright + "\n\n" + e.getMessage)
      case e: java.net.UnknownHostException => failureExit(e.toString, ExitStatus.NoHost)
      case e: java.net.ConnectException => failureExit(e.toString, ExitStatus.Unavailable)
      case e: java.io.IOException => failureExit(e.toString, ExitStatus.IoErr)
    }
  }

  val mainUncaughtExceptionHandler = basicUncaughtExceptionHandler

}

class LeaderLocation(val runtimeId: DOrcRuntime.RuntimeId, val connection: RuntimeConnection[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd, DOrcExecution, PeerLocation]) extends RuntimeRef[OrcFollowerToLeaderCmd] with ClosableConnection {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"
  override def send(message: OrcFollowerToLeaderCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcFollowerToLeaderCmd): Unit = connection.sendInContext(execution, this)(message)
  override def close(): Unit = connection.close()
  override def abort(): Unit = connection.abort()
}

class PeerLocationImpl(val runtimeId: DOrcRuntime.RuntimeId, val connection: RuntimeConnection[OrcPeerCmd, OrcPeerCmd, DOrcExecution, PeerLocation], val listenAddress: InetSocketAddress) extends PeerLocation with ClosableConnection {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId, listenAddress=$listenAddress)"
  override def send(message: OrcPeerCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcPeerCmd): Unit = connection.sendInContext(execution, this)(message)
  override def close(): Unit = connection.close()
  override def abort(): Unit = connection.abort()
}
