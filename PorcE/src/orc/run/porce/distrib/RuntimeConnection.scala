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

import orc.util.{ ConnectionListener, EventCounter, SocketObjectConnection }

/** A connection between DOrcRuntimes.  Extends SocketObjectConnection to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
class RuntimeConnection[+R, -S](socket: Socket) extends SocketObjectConnection[R, S](socket) {
  var lastReset = System.currentTimeMillis()
  val RESET_PERIOD = 5 * 1000

  /* Note: Always get output before input */
  val cos = new CountingOutputStream(socket.getOutputStream())
  override val oos = new RuntimeConnectionOutputStream(cos)
  override val ois = new RuntimeConnectionInputStream(socket.getInputStream())

  override def receive(): R = {
    val obj = super.receive()
    Logger.finest(s"${Console.BLUE}RuntimeConnection.receive: Received $obj on $socket${Console.RESET}")
    EventCounter.count( /*'receive*/ Symbol("recv " + obj.getClass.getName))
    obj
  }

  override def send(obj: S): Unit = {
    Logger.finest(s"${Console.RED}RuntimeConnection.send: Sending $obj on $socket${Console.RESET}")
    val startCount = cos.bytecount
    super.send(obj)
    Logger.finest(s"message size = ${cos.bytecount - startCount}")
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

  def sendInContext(execution: DOrcExecution, destination: PeerLocation)(obj: S): Unit = oos synchronized {
    oos.setContext(execution, destination)
    try {
      send(obj)
    } finally {
      oos.clearContext()
    }
    maybeReset()
  }
  
  private def maybeReset() = {
    val now = System.currentTimeMillis()    
    if(now > lastReset + RESET_PERIOD) {
      oos.reset()
      lastReset = now
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

  override def acceptConnection(): RuntimeConnection[R, S] = {
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

  def apply[R, S](remoteSockAddr: InetSocketAddress, localSockAddr: InetSocketAddress = null): RuntimeConnection[R, S] = {
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
        Logger.finest(s"ExecutionContext ${obj.getClass.getName}=$obj, xid=${xm.executionId}")
        currExecution = Some((currExecutionLookup.get)(xm.executionId))
        obj
      }
      case _ if currExecution.isDefined && currOrigin.isDefined && currExecution.get.unmarshalExecutionObject.isDefinedAt((currOrigin.get, obj)) =>
        currExecution.get.unmarshalExecutionObject((currOrigin.get, obj))
//      case _ if currExecution.isDefined /*&& currExecution.get.unmarshalValueWouldReplace(obj)*/ => currExecution.get.unmarshalValue(obj)
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
    //Logger.entering(getClass.getName, "replaceObject", Seq(s"${obj.getClass.getName}=$obj"))
    val result = obj match {
      case xm: ExecutionContextSerializationMarker => {
        assert(xm.executionId == currExecution.get.executionId)
        obj
      }
      case _ if currExecution.isDefined && currDestination.isDefined && currExecution.get.marshalExecutionObject.isDefinedAt((currDestination.get, obj)) =>
        currExecution.get.marshalExecutionObject((currDestination.get, obj))
//      case _ if currExecution.isDefined && currDestination.isDefined /*&& currExecution.get.marshalValueWouldReplace(currDestination.get)(obj)*/ =>
//          currExecution.get.marshalValue(currDestination.get)(obj)
      case _ => super.replaceObject(obj)
    }
    //Logger.exiting(getClass.getName, "replaceObject", s"${result.getClass.getName}=$result")
    result
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

}

/** OutputStream wrapper that counts bytes written to the stream.
  *
  * @author jthywiss
  */
class CountingOutputStream(out: OutputStream) extends OutputStream {
  var bytecount: Long = 0L

  @throws(classOf[IOException])
  override def write(b: Int): Unit = {
    bytecount += 1
    out.write(b)
  }

  @throws(classOf[IOException])
  override def write(b: Array[Byte]): Unit = {
    bytecount += b.length
    out.write(b, 0, b.length)
  }

  @throws(classOf[IOException])
  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    bytecount += len
    out.write(b, off, len)
  }

  @throws(classOf[IOException])
  override def flush(): Unit = out.flush()

  @throws(classOf[IOException])
  override def close(): Unit = out.close()
}
