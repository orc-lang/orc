//
// FollowerRuntime.scala -- Scala classes FollowerRuntime, LeaderLocation, and PeerLocationImpl; and trait ClosableConnection
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

import java.io.{ EOFException, IOException }
import java.net.{ InetSocketAddress, SocketException }
import java.util.logging.Level

import scala.collection.JavaConverters.mapAsScalaConcurrentMap
import scala.util.control.NonFatal

import orc.{ OrcEvent, OrcExecutionOptions }
import orc.util.CmdLineParser

/** Orc runtime engine running as part of a dOrc cluster.
  *
  * @author jthywiss
  */
class FollowerRuntime(runtimeId: DOrcRuntime#RuntimeId, listenAddress: InetSocketAddress) extends DOrcRuntime(runtimeId, s"dOrc $runtimeId @ ${listenAddress.toString()}") {

  protected var listener: RuntimeConnectionListener[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd] = null
  protected var boundListenAddress: InetSocketAddress = null
  protected val runtimeLocationMap = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcRuntime#RuntimeId, PeerLocation]())

  override def locationForRuntimeId(runtimeId: DOrcRuntime#RuntimeId): PeerLocation = runtimeLocationMap(runtimeId)

  override def allLocations: Set[PeerLocation] = runtimeLocationMap.values.toSet

  def listen(): Unit = {
    Logger.info(s"Listening on $listenAddress")

    runtimeLocationMap.put(runtimeId, here)

    listener = new RuntimeConnectionListener[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd](listenAddress)
    boundListenAddress = new InetSocketAddress(listener.serverSocket.getInetAddress, listener.serverSocket.getLocalPort)
    try {
      while (!listener.serverSocket.isClosed()) {
        val newConn = listener.acceptConnection()
        Logger.finer(s"accepted ${newConn.socket}")

        MessageProcessorThread(newConn).start()

      }
    } catch {
      case se: SocketException if se.getMessage == "Socket closed" => /* Ignore */
      case se: SocketException if se.getMessage == "Connection reset" => /* Ignore */
    } finally {
      runtimeLocationMap.valuesIterator.foreach {
        _ match {
          case a: ClosableConnection => a.abort()
          case _ => /* Ignore */
        }
      }
      runtimeLocationMap.clear()
      listener.close()
      Logger.info(s"Closed $listenAddress")
      boundListenAddress = null
    }
  }

  protected class MessageProcessorThread[R <: OrcCmd, S <: OrcCmd](connection: RuntimeConnection[R, S], initiatingWithRuntimeId: Option[DOrcRuntime#RuntimeId])
    extends Thread(s"dOrc follower connection with ${connection.socket}") {

    override def run(): Unit = {
      Logger.entering(getClass.getName, "run")
      try {
        initiatingWithRuntimeId match {
          case Some(otherId) => connection.send(DOrcConnectionHeader(runtimeId, otherId).asInstanceOf[S])
          case None =>
        }

        connection.receive() match {
          case DOrcConnectionHeader(0, rid) if (rid == runtimeId && (initiatingWithRuntimeId == None || initiatingWithRuntimeId.get == 0)) => {
            setName(s"dOrc follower receiver for leader @ ${connection.socket}")
            /* Ugly: The ids in the connection header tell us the type of the subsequent commands */
            val newConnAsLeaderConn = connection.asInstanceOf[RuntimeConnection[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd]]
            val leaderLocation = new LeaderLocation(0, newConnAsLeaderConn)
            val oldMappedValue = runtimeLocationMap.put(0, leaderLocation)
            assert(oldMappedValue == None)

            initiatingWithRuntimeId match {
              case Some(_) =>
              case None => connection.send(DOrcConnectionHeader(runtimeId, 0).asInstanceOf[S])
            }
            followLeader(leaderLocation)
          }
          case DOrcConnectionHeader(sid, rid) if (rid == runtimeId && (initiatingWithRuntimeId == None || initiatingWithRuntimeId.get == sid)) => {
            setName(f"dOrc follower receiver for peer $sid%#x @ ${connection.socket}")
            /* Ugly: The ids in the connection header tell us the type of the subsequent commands */
            val newConnAsPeerConn = connection.asInstanceOf[RuntimeConnection[OrcPeerCmd, OrcPeerCmd]]
            val newPeerLoc = new PeerLocationImpl(sid, newConnAsPeerConn)
            val oldMappedValue = runtimeLocationMap.put(sid, newPeerLoc)
            assert(oldMappedValue == None)

            initiatingWithRuntimeId match {
              case Some(_) =>
              case None => connection.send(DOrcConnectionHeader(runtimeId, sid).asInstanceOf[S])
            }
            communicateWithPeer(sid, newPeerLoc)
          }
          case DOrcConnectionHeader(sid, rid) => throw new AssertionError(f"Received DOrcConnectionHeader with wrong runtime ids: sender=$sid%#x, receiver=$rid%#x")
          case m => throw new AssertionError(s"Received message before DOrcConnectionHeader: $m")
        }
      } catch {
        case e: Throwable => {
          Logger.log(Level.SEVERE, "MessageProcessorThread caught", e);
          throw e
        }
      } finally {
        Logger.info(s"Stopped reading events from ${connection.socket}")
        if (!connection.closed) {
          try {
            Logger.fine(s"MessageProcessorThread terminating; aborting $connection")
            connection.abort()
          } catch {
            case NonFatal(e2) => Logger.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
        }
      }
      Logger.exiting(getClass.getName, "run")
    }
  }

  protected object MessageProcessorThread {
    def apply(connection: RuntimeConnection[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd]) = new MessageProcessorThread(connection, None)
    def apply(connection: RuntimeConnection[OrcPeerCmd, OrcPeerCmd], initiatingWithRuntimeId: DOrcRuntime#RuntimeId) = new MessageProcessorThread(connection, Some(initiatingWithRuntimeId))
  }

  protected def followLeader(leaderLocation: LeaderLocation): Unit = {
    Logger.entering(getClass.getName, "followLeader")
    try {
      var done = false

      Logger.info(s"Reading events from ${leaderLocation.connection.socket}")

      while (!done && !leaderLocation.connection.closed && !leaderLocation.connection.socket.isInputShutdown) {
        val cmd = try {
          leaderLocation.connection.receiveInContext({ programs(_) }, leaderLocation)
        } catch {
          case _: EOFException => EOF
        }
        //Logger.finest(s"received $cmd")
        cmd match {
          case AddPeerCmd(peerRuntimeId, peerListenAddress) => addPeer(peerRuntimeId, peerListenAddress)
          case RemovePeerCmd(peerRuntimeId) => removePeer(peerRuntimeId)
          case LoadProgramCmd(xid, oil, options, rootCounterId) => loadProgram(leaderLocation, xid, oil, options, rootCounterId)
          case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(leaderLocation, gmpid, movedCall)
          case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(leaderLocation, gmpid, pub)
          case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
          case KillGroupCmd(xid, gpid) => programs(xid).killGroupProxy(gpid)
          case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
          case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid)
          case ReadFutureCmd(xid, futureId, readerFollowerNum) => programs(xid).readFuture(futureId, readerFollowerNum)
          case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(futureId, value)
          case UnloadProgramCmd(xid) => unloadProgram(xid)
          case EOF => done = true
          case DOrcConnectionHeader(sid, rid) => throw new AssertionError(f"Received extraneous DOrcConnectionHeader: sender=$sid%#x, receiver=$rid%#x")
        }
      }
    } finally {
      if (!programs.isEmpty) Logger.warning(s"Shutting down with ${programs.size} programs still loaded: ${programs.keys.mkString(",")}")
      programs.clear()
      stopScheduler()
      runtimeLocationMap.remove(0)
      FollowerRuntime.this.stop()
    }
    Logger.finer(s"runtimeLocationMap.size = ${runtimeLocationMap.size}; runtimeLocationMap = $runtimeLocationMap")
    Logger.exiting(getClass.getName, "followLeader")
  }

  protected def communicateWithPeer(peerRuntimeId: DOrcRuntime#RuntimeId, peerLocation: PeerLocationImpl): Unit = {
    try {
      Logger.info(s"Reading events from ${peerLocation.connection.socket}")
      while (!peerLocation.connection.closed && !peerLocation.connection.socket.isInputShutdown) {
        val msg = try {
          peerLocation.connection.receiveInContext({ programs(_) }, peerLocation)
        } catch {
          case _: EOFException => EOF
        }
        Logger.finest(s"Read ${msg}")
        msg match {
          case MigrateCallCmd(xid, gmpid, movedCall) => programs(xid).receiveCall(peerLocation, gmpid, movedCall)
          case PublishGroupCmd(xid, gmpid, pub) => programs(xid).publishInGroup(peerLocation, gmpid, pub)
          case KillGroupCmd(xid, gpid) => programs(xid).killGroupProxy(gpid)
          case HaltGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).haltGroupMemberProxy(gmpid, n)
          case DiscorporateGroupMemberProxyCmd(xid, gmpid, n) => programs(xid).discorporateGroupMemberProxy(gmpid, n)
          case ResurrectGroupMemberProxyCmd(xid, gmpid) => programs(xid).resurrectGroupMemberProxy(gmpid)
          case ReadFutureCmd(xid, futureId, readerFollowerNum) => programs(xid).readFuture(futureId, readerFollowerNum)
          case DeliverFutureResultCmd(xid, futureId, value) => programs(xid).deliverFutureResult(futureId, value)
          case EOF => { Logger.fine(s"EOF, aborting peerLocation"); peerLocation.connection.abort() }
          case DOrcConnectionHeader(sid, rid) => throw new AssertionError(f"Received extraneous DOrcConnectionHeader: sender=$sid%#x, receiver=$rid%#x")
        }
      }
    } finally {
      runtimeLocationMap.remove(peerRuntimeId)
    }
  }

  def sendEvent(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId, groupProxyId: RemoteRef#RemoteRefId)(event: OrcEvent): Unit = {
    Logger.entering(getClass.getName, "sendEvent")
    val execution = programs(executionId)
    try {
      Tracer.traceOrcEventSend(here, leaderLocation)
      leaderLocation.sendInContext(execution)(NotifyLeaderCmd(executionId, event))
    } catch {
      case e1: SocketException => {
        if (!leaderLocation.connection.closed) {
          try {
            Logger.fine(s"SocketException, aborting $leaderLocation")
            leaderLocation.connection.abort()
          } catch {
            case e2: IOException => Logger.finer(s"Ignoring $e2") /* Ignore close failures at this point */
          }
          throw e1
        } else {
          Logger.finer(s"Ignoring $e1") /* Ignore failures on a closed socket */
        }
      }
    }
    Logger.exiting(getClass.getName, "sendEvent")
  }

  def addPeer(peerRuntimeId: DOrcRuntime#RuntimeId, peerListenAddress: InetSocketAddress): Unit = {
    Logger.entering(getClass.getName, "addPeer", Seq(peerRuntimeId.toString, peerListenAddress))
    if (peerListenAddress == boundListenAddress || peerRuntimeId == runtimeId) {
      /* Hey, that's me! */
      assert(peerRuntimeId == runtimeId)
      assert(runtimeLocationMap(peerRuntimeId) == here)
    } else {
      if (shouldOpen(peerListenAddress)) {
        Logger.finest("New peer; Will open connection")

        /* FIXME:Race: If two addPeer calls for the same peerRuntimeId run simultaneously, an assertion will fail in MessageProcessorThread.run */
        runtimeLocationMap.get(peerRuntimeId) match {
          case Some(pl: PeerLocationImpl) if (pl.connection.socket.getRemoteSocketAddress == peerListenAddress) => {
            Logger.fine(s"addPeer: redundant add of $peerRuntimeId => $peerListenAddress")
          }
          case Some(pl) => throw new AssertionError(s"addPeer: adding $peerRuntimeId => $peerListenAddress, but $peerRuntimeId is already mapped to $pl")
          case None => {
            Logger.finest("Opening connection")
            MessageProcessorThread(RuntimeConnectionInitiator[OrcPeerCmd, OrcPeerCmd](peerListenAddress), peerRuntimeId).start()
          }
        }

      } else {
        Logger.finest("Expecting peer to initiate connection (it may have already)")
      }
    }
    Logger.exiting(getClass.getName, "addPeer")
  }

  protected def shouldOpen(peerListenAddress: InetSocketAddress): Boolean = {
    /* Convention: Peer with "lower" address initiates connection */
    val bla = (boundListenAddress.getAddress.getAddress map { _.toInt }) :+ boundListenAddress.getPort
    val pla = (peerListenAddress.getAddress.getAddress map { _.toInt }) :+ peerListenAddress.getPort
    scala.math.Ordering.Iterable[Int].lt(bla, pla)
  }

  def removePeer(peerRuntimeId: DOrcRuntime#RuntimeId): Unit = {
    Logger.entering(getClass.getName, "removePeer", Seq(peerRuntimeId.toString))
    if (peerRuntimeId == runtimeId) {
      /* If the leader sends RemovePeer with our ID, we shutdown */
      Logger.fine("Closing listen socket")
      listener.close()
    } else {
      runtimeLocationMap.remove(peerRuntimeId) match {
        case Some(removedLocation: LeaderLocation) => /* leave leader connection open */
        case Some(removedLocation: ClosableConnection) => removedLocation.abort()
        case _ => /* Nothing to do */
      }
    }
  }

  val programs = mapAsScalaConcurrentMap(new java.util.concurrent.ConcurrentHashMap[DOrcExecution#ExecutionId, DOrcFollowerExecution])

  def loadProgram(leaderLocation: LeaderLocation, executionId: DOrcExecution#ExecutionId, programAst: DOrcRuntime#ProgramAST, options: OrcExecutionOptions, rootCounterId: CounterProxyManager#CounterProxyId): Unit = {
    Logger.entering(getClass.getName, "loadProgram")

    assert(programs.isEmpty) /* For now */
    if (programs.isEmpty) {
      Logger.fine("starting scheduler")
      startScheduler(options)
    }

    val followerExecution = 
      new DOrcFollowerExecution(executionId, runtimeId, programAst, options,
          sendEvent(leaderLocation, executionId, DOrcExecution.noGroupProxyId),
          rootCounterId,leaderLocation, this)
    installHandlers(followerExecution)

    programs.put(executionId, followerExecution)
    addRoot(followerExecution)
  }

  def unloadProgram(executionId: DOrcExecution#ExecutionId): Unit = {
    Logger.entering(getClass.getName, "unloadProgram", Seq(executionId))
    programs.remove(executionId) match {
      case None => Logger.warning(s"Received unload for unknown (or previously unloaded) execution $executionId")
      case Some(removedExecution) =>
        // FIXME: Figure out how to implement this warning. Currently there is no way to tell if a follower execution is completed since it doesn't have a single root counter.
        //if (!removedExecution.isDone) {
        //  Logger.fine(s"Unloaded $executionId with group members still in execution")
        //}
    }

    assert(programs.isEmpty) /* For now */
    if (programs.isEmpty) {
      stopScheduler()
      super.stop()
    }
  }

  val here = Here

  object Here extends PeerLocation {
    override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"
    override def send(message: OrcPeerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
    override def sendInContext(execution: DOrcExecution)(message: OrcPeerCmd): Unit = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
    override def runtimeId: DOrcRuntime#RuntimeId = FollowerRuntime.this.runtimeId
  }

}

object FollowerRuntime {

  def main(args: Array[String]): Unit = {
    val frOptions = new FollowerRuntimeCmdLineOptions()
    frOptions.parseCmdLine(args)

    val orcLogger = java.util.logging.Logger.getLogger("orc")
    val logLevel = java.util.logging.Level.parse(frOptions.logLevel)
    setupLogging(orcLogger, logLevel)

    new FollowerRuntime(frOptions.runtimeId, frOptions.socket).listen()
  }

  def setupLogging(orcLogger: java.util.logging.Logger, logLevel: java.util.logging.Level): Unit = {
    orcLogger.setLevel(logLevel)
    val testOrcLogRecord = new java.util.logging.LogRecord(logLevel, "")
    testOrcLogRecord.setLoggerName(orcLogger.getName())
    def willLog(checkLogger: java.util.logging.Logger, testLogRecord: java.util.logging.LogRecord): Boolean = {
      for (handler <- checkLogger.getHandlers()) {
        if (handler.isLoggable(testLogRecord))
          return true
      }
      if (checkLogger.getUseParentHandlers() && checkLogger.getParent() != null) {
        return willLog(checkLogger.getParent(), testLogRecord)
      } else {
        return false
      }
    }
    if (!willLog(orcLogger, testOrcLogRecord)) {
      /* Only add handler if no existing handler (here or in parents) is at our logging level */
      val oldLoggers = orcLogger.getHandlers()
      val logHandler = new java.util.logging.ConsoleHandler()
      logHandler.setLevel(logLevel)
      logHandler.setFormatter(orc.util.SyslogishFormatter)
      orcLogger.addHandler(logHandler)
      orcLogger.warning(s"No log handler found for 'orc' $logLevel log records, so a ConsoleHandler was added.  This may result in duplicate log records. The old handlers are: ${oldLoggers.toSeq}")
    }
    orcLogger.config(orc.Main.orcImplName + " " + orc.Main.orcVersion)
    orcLogger.config("Orc logging level: " + logLevel)
  }

}

class FollowerRuntimeCmdLineOptions() extends CmdLineParser {
  private var runtimeId_ = 0
  def logLevel: String = logLevel_
  def logLevel_=(newVal: String): Unit = logLevel_ = newVal
  private var socket_ : InetSocketAddress = null
  def socket: InetSocketAddress = socket_
  def socket_=(newVal: InetSocketAddress): Unit = socket_ = newVal
  private var logLevel_ = "INFO"
  def runtimeId: Int = runtimeId_
  def runtimeId_=(newVal: Int): Unit = runtimeId_ = newVal

  IntOprd(() => runtimeId, runtimeId = _, position = 0, argName = "runtime-id", required = true, usage = "d-Orc runtime (follower) ID")

  SocketOprd(() => socket, socket = _, position = 1, argName = "socket", required = true, usage = "Local socket (host:port) to listen on")

  StringOpt(() => logLevel, logLevel = _, ' ', "loglevel", usage = "Set the level of logging. Default is INFO. Allowed values: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL")
}

trait ClosableConnection {
  def close(): Unit
  def abort(): Unit
}

class LeaderLocation(val runtimeId: DOrcRuntime#RuntimeId, val connection: RuntimeConnection[OrcLeaderToFollowerCmd, OrcFollowerToLeaderCmd]) extends Location[OrcFollowerToLeaderCmd] with ClosableConnection {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"
  override def send(message: OrcFollowerToLeaderCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcFollowerToLeaderCmd): Unit = connection.sendInContext(execution, this)(message)
  override def close(): Unit = connection.close()
  override def abort(): Unit = connection.abort()
}

class PeerLocationImpl(val runtimeId: DOrcRuntime#RuntimeId, val connection: RuntimeConnection[OrcPeerCmd, OrcPeerCmd]) extends PeerLocation with ClosableConnection {
  override def toString: String = s"${getClass.getName}(runtimeId=$runtimeId)"
  override def send(message: OrcPeerCmd): Unit = connection.send(message)
  override def sendInContext(execution: DOrcExecution)(message: OrcPeerCmd): Unit = connection.sendInContext(execution, this)(message)
  override def close(): Unit = connection.close()
  override def abort(): Unit = connection.abort()
}
