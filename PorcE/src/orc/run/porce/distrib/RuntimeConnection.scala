//
// RuntimeConnection.scala -- Scala classes RuntimeConnection, RuntimeConnectionListener, ExecutionContextSerializationMarker, RuntimeConnectionInputStream, RuntimeConnectionOutputStream, and ClosureReplacement; and objects RuntimeConnectionInitiator and ClosureReplacement
// Project PorcE
//
// Created by jthywiss on Jul 12, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import java.io.{ IOException, InputStream, ObjectInputStream, ObjectOutputStream, OutputStream }
import java.net.{ InetAddress, InetSocketAddress, Socket }

import com.oracle.truffle.api.RootCallTarget

import orc.util.{ ConnectionListener, EventCounter, SocketObjectConnection }

/** A connection between DOrcRuntimes.  Extends SocketObjectConnection to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
class RuntimeConnection[+R, -S](socket: Socket) extends SocketObjectConnection[R, S](socket) {

  /* Note: Always get output before input */
  override val oos = new RuntimeConnectionOutputStream(socket.getOutputStream())
  override val ois = new RuntimeConnectionInputStream(socket.getInputStream())

  override def receive(): R = {
    val obj = super.receive()
    Logger.finest(s"${Console.BLUE}RuntimeConnection.receive: Received $obj on $socket${Console.RESET}")
    EventCounter.count( /*'receive*/ Symbol("recv " + obj.getClass.getName))
    obj
  }

  override def send(obj: S): Unit = {
    Logger.finest(s"${Console.RED}RuntimeConnection.send: Sending $obj on $socket${Console.RESET}")
    super.send(obj)
    EventCounter.count( /*'send*/ Symbol("send " + obj.getClass.getName))
  }

  def receiveInContext(executionLookup: (DOrcExecution#ExecutionId) => DOrcExecution, origin: PeerLocation)(): R = ois synchronized {
    ois.setContext(executionLookup, origin)
    try {
      receive()
    } finally {
      ois.clearContext()
    }
  }

  def sendInContext(execution: DOrcExecution, destination: PeerLocation)(obj: S) = oos synchronized {
    oos.setContext(execution, destination)
    try {
      send(obj)
    } finally {
      oos.clearContext()
    }
  }

  override def close(): Unit = {
    Logger.finest(s"${Console.GREEN}RuntimeConnection.close on $socket${Console.RESET}")
    super.close()
  }

  override def abort(): Unit = {
    Logger.finest(s"${Console.GREEN}RuntimeConnection.abort on $socket${Console.RESET}")
    super.abort()
  }

}

/** Listens for incoming dOrc RuntimeConnections.  Extends ConnectionListener
  * to provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
class RuntimeConnectionListener[+R, -S](bindSockAddr: InetSocketAddress) extends ConnectionListener[R, S](bindSockAddr) {

  override def acceptConnection() = {
    val acceptedSocket = serverSocket.accept()
    Logger.finer(s"${Console.GREEN}RuntimeConnectionListener accepted $acceptedSocket${Console.RESET}")
    SocketObjectConnection.configSocket(acceptedSocket)
    new RuntimeConnection[R, S](acceptedSocket)
  }

  Logger.finer(s"${Console.GREEN}RuntimeConnectionListener listening on $serverSocket${Console.RESET}")

}

/** Actively opens dOrc RuntimeConnections.  Replaces ConnectionInitiator to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
object RuntimeConnectionInitiator {

  def apply[R, S](remoteSockAddr: InetSocketAddress, localSockAddr: InetSocketAddress = null) = {
    val socket = new Socket()
    SocketObjectConnection.configSocket(socket)
    if (localSockAddr != null) {
      socket.bind(localSockAddr)
    }
    socket.connect(remoteSockAddr)
    Logger.finer(s"${Console.GREEN}RuntimeConnectionInitiator opening $socket${Console.RESET}")
    new RuntimeConnection[R, S](socket)
  }

  def apply[R, S](remoteHostAddr: InetAddress, remotePort: Int): SocketObjectConnection[R, S] = apply[R, S](new InetSocketAddress(remoteHostAddr, remotePort))

  def apply[R, S](remoteHostname: String, remotePort: Int): SocketObjectConnection[R, S] = apply[R, S](new InetSocketAddress(remoteHostname, remotePort))

}

/** Marks the execution ID in an OrcPeerCmd so that RuntimeConnection can
  * recognize it during deserialization.
  *
  * Note that a fresh instance of ExecutionContextSerializationMarker should
  * be written in each sent OrcPeerCmd.
  *
  * @see OrcPeerCmd
  * @author jthywiss
  */
class ExecutionContextSerializationMarker(val executionId: DOrcExecution#ExecutionId) extends Serializable

/** Deserializes Orc/dOrc values previously written using an
  * RuntimeConnectionOutputStream.  Extends ObjectInputStream to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
protected class RuntimeConnectionInputStream(in: InputStream) extends ObjectInputStream(in) {

  enableResolveObject(true)

  @throws(classOf[IOException])
  override protected def resolveObject(obj: AnyRef): AnyRef = {
    obj match {
      case xm: ExecutionContextSerializationMarker => {
        currExecution = Some((currExecutionLookup.get)(xm.executionId))
        obj
      }
      case RootCallTargetReplacement(index) =>
        currExecution.get.idToCallTarget(index)
      ///* ClosureReplacement: see ClosureReplacement.readResolve() */
      //case cgr: ClosureGroupReplacement => resolveClosureGroup(cgr)
      case _ => super.resolveObject(obj)
    }
  }

  protected var currExecutionLookup: Option[(DOrcExecution#ExecutionId) => DOrcExecution] = None
  protected var currOrigin: Option[PeerLocation] = None
  protected var currExecution: Option[DOrcExecution] = None

  def setContext(executionLookup: (DOrcExecution#ExecutionId) => DOrcExecution, origin: PeerLocation): Unit = {
    assert(currExecutionLookup.isEmpty && currOrigin.isEmpty, s"currExecutionLookup=$currExecutionLookup; currOrigin=$currOrigin")
    currExecutionLookup = Some(executionLookup)
    currOrigin = Some(origin)
  }

  def clearContext(): Unit = {
    assert(currExecutionLookup.nonEmpty && currOrigin.nonEmpty, s"currExecutionLookup=$currExecutionLookup; currOrigin=$currOrigin")
    currExecutionLookup = None
    currOrigin = None
    currExecution = None
  }

  //def resolveClosureGroup(cgr: ClosureGroupReplacement) = {
  //  cgr.unmarshalClosureGroup(currExecution.get, currOrigin.get)
  //}

}

/** Writes Orc/dOrc values to an OutputStream.  The values can be read
  * (reconstituted) using an RuntimeConnectionInputStream.  Extends
  * ObjectOutputStream to provide extra serialization support for Orc/dOrc
  * values.
  *
  * @author jthywiss
  */
protected class RuntimeConnectionOutputStream(out: OutputStream) extends ObjectOutputStream(out) {

  enableReplaceObject(true)

  @throws(classOf[IOException])
  override protected def replaceObject(obj: AnyRef): AnyRef = {
    obj match {
      case xm: ExecutionContextSerializationMarker => {
        assert(xm.executionId == currExecution.get.executionId)
        obj
      }
      case ct: RootCallTarget =>
        RootCallTargetReplacement(currExecution.get.callTargetToId(ct))
      //case c: Closure => replaceClosure(c)
      //case cg: ClosureGroup => replaceClosureGroup(cg)
      case _ => super.replaceObject(obj)
    }
  }

  protected var currExecution: Option[DOrcExecution] = None
  protected var currDestination: Option[PeerLocation] = None

  def setContext(execution: DOrcExecution, destination: PeerLocation): Unit = {
    assert(currExecution.isEmpty && currDestination.isEmpty, s"currExecution=$currExecution; currDestination=$currDestination")
    currExecution = Some(execution)
    currDestination = Some(destination)
  }

  def clearContext(): Unit = {
    assert(currExecution.nonEmpty && currDestination.nonEmpty, s"currExecution=$currExecution; currDestination=$currDestination")
    currExecution = None
    currDestination = None
  }

  //def replaceClosure(c: Closure) = ClosureReplacement(c)

  //def replaceClosureGroup(cg: ClosureGroup) = {
  //  TokenFieldMarshaling.marshalClosureGroup(cg, currExecution.get, currDestination.get)
  //}

}

protected final case class RootCallTargetReplacement(index: Int)

///** Replacement for a Closure for use in serialization.
//  *
//  * @author jthywiss
//  */
//protected case class ClosureReplacement(index: Int, closureGroup: ClosureGroup) {
//  @throws(classOf[ObjectStreamException])
//  protected def readResolve(): AnyRef = new Closure(index, closureGroup)
//}
//
//protected object ClosureReplacement {
//  def apply(c: Closure) = new ClosureReplacement(c.index, c.closureGroup)
//}
