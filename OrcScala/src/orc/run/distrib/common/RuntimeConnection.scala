//
// RuntimeConnection.scala -- Scala classes RuntimeConnection, RuntimeConnectionListener, ExecutionContextSerializationMarker, RuntimeConnectionInputStream, and RuntimeConnectionOutputStream; trait ExecutionMarshaling; and object RuntimeConnectionInitiator
// Project OrcScala
//
// Created by jthywiss on Jul 12, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

import java.io.{ EOFException, IOException, InputStream, ObjectInputStream, ObjectOutputStream, OutputStream }
import java.net.{ ConnectException, InetAddress, InetSocketAddress, Socket }
import java.util.logging.Level

import orc.run.distrib.Logger
import orc.util.{ ConnectionListener, EventCounter, ServerSocketWithWriteTimeout, SocketObjectConnection, SocketWithWriteTimeout }

/** A connection between DOrcRuntimes.  Extends SocketObjectConnection to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
class RuntimeConnection[+ReceivableMessage, -SendableMessage, Execution <: ExecutionMarshaling[Location], Location](socket: Socket) extends SocketObjectConnection[ReceivableMessage, SendableMessage](socket) {
  private var lastObjectStreamReset = System.currentTimeMillis()

  /* Note: Always get output before input */
  val cos = new CountingOutputStream(socket.getOutputStream())
  override val oos = new RuntimeConnectionOutputStream[Execution, Location, Execution#ExecutionId](cos)
  override val ois = new RuntimeConnectionInputStream[Execution, Location, Execution#ExecutionId](socket.getInputStream())

  override def receive(): ReceivableMessage = {
    val obj = try {
      super.receive()
    } catch {
      case e: EOFException => throw e
      case e: Throwable => {
        Logger.Message.log(Level.SEVERE, s"RuntimeConnection.receive: $e thrown when reading from $socket", e)
        throw e
      }
    }
    Logger.Message.finest(s"RuntimeConnection.receive: Received $obj on $socket")
    EventCounter.count(Symbol("recv " + orc.util.GetScalaTypeName(obj)))
    obj
  }

  override def send(obj: SendableMessage): Unit = {
    Logger.Message.finest(s"RuntimeConnection.send: Sending $obj on $socket")
    val startCount = cos.bytecount
    try {
      super.send(obj)
    } catch {
      case srfe: SerializationReplacementFailureException => /* Unwrap */ throw srfe.getCause
    }
    Logger.Message.finest(s"message size = ${cos.bytecount - startCount}")
    EventCounter.count(Symbol("send " + orc.util.GetScalaTypeName(obj)))
    maybeReset()
  }

  def receiveInContext(executionLookup: (Execution#ExecutionId) => Execution, origin: Location)(): ReceivableMessage = ois synchronized {
    ois.setContext(executionLookup, origin)
    try {
      receive()
    } finally {
      ois.clearContext()
    }
  }

  def sendInContext(execution: Execution, destination: Location)(obj: SendableMessage): Unit = oos synchronized {
    oos.setContext(execution, destination)
    try {
      send(obj)
    } finally {
      oos.clearContext()
    }
  }

  private def maybeReset() = {
    /*FIXME:HACK: Clear ObjectOutputStream replacement and handle maps to reduce object leaks. 
     * This breaks reference graph integrity, and leads to periodic resending of duplicate objects.
     * Time to manage our own handles? */
    val now = System.currentTimeMillis()
    if (now > lastObjectStreamReset + RuntimeConnection.objectStreamResetPeriod) {
      oos.reset()
      lastObjectStreamReset = now
    }
  }

  override def close(): Unit = {
    Logger.Connect.finest(s"RuntimeConnection.close on $socket")
    super.close()
  }

  override def abort(): Unit = {
    Logger.Connect.finest(s"RuntimeConnection.abort on $socket")
    super.abort()
  }

}

object RuntimeConnection {
  val objectStreamResetPeriod = System.getProperty("orc.distrib.objectStreamResetPeriod", "5000").toLong /* ms */
  val socketWriteTimeout = System.getProperty("orc.distrib.socketWriteTimeout", "300000").toLong /* ms = 5 min */  
}

/** Listens for incoming dOrc RuntimeConnections.  Extends ConnectionListener
  * to provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
class RuntimeConnectionListener[+ReceivableMessage, -SendableMessage, Execution <: ExecutionMarshaling[Location], Location](bindSockAddr: InetSocketAddress) extends ConnectionListener[ReceivableMessage, SendableMessage](bindSockAddr) {

  override protected def newServerSocket() = new ServerSocketWithWriteTimeout(RuntimeConnection.socketWriteTimeout)

  override def acceptConnection(): RuntimeConnection[ReceivableMessage, SendableMessage, Execution, Location] = {
    val acceptedSocket = serverSocket.accept()
    Logger.Connect.finer(s"RuntimeConnectionListener accepted $acceptedSocket")
    SocketObjectConnection.configSocket(acceptedSocket)
    new RuntimeConnection[ReceivableMessage, SendableMessage, Execution, Location](acceptedSocket)
  }

  Logger.Connect.finer(s"RuntimeConnectionListener listening on $serverSocket")

}

/** Actively opens dOrc RuntimeConnections.  Replaces ConnectionInitiator to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
object RuntimeConnectionInitiator {

  def apply[ReceivableMessage, SendableMessage, Execution <: ExecutionMarshaling[Location], Location](remoteSockAddr: InetSocketAddress, localSockAddr: InetSocketAddress = null, retryTimes: Int = 25, retryPeriodInitial: Int = 400): RuntimeConnection[ReceivableMessage, SendableMessage, Execution, Location] = {
    var retryDelay = retryPeriodInitial
    for (retryNum <- 0 to retryTimes) {
      val rc = try {
        val socket = newSocket()
        SocketObjectConnection.configSocket(socket)
        if (localSockAddr != null) {
          socket.bind(localSockAddr)
        }
        socket.connect(remoteSockAddr)
        Logger.Connect.finer(s"RuntimeConnectionInitiator opening $socket")
        new RuntimeConnection[ReceivableMessage, SendableMessage, Execution, Location](socket)
      } catch {
        case ce: ConnectException if retryNum < retryTimes =>
          Logger.Connect.log(Level.FINEST, s"Caught ConnectException when connecting to $remoteSockAddr, will retry in $retryDelay ms", ce)
          null
        case ioe: IOException =>
          throw new IOException(s"Unable to connect to $remoteSockAddr: ${ioe.getMessage}", ioe)
      }
      if (rc != null) return rc
      Thread.sleep(retryDelay)
      retryDelay = retryDelay + (retryDelay / 2)
    }
    throw new AssertionError("Control should not reach this point")
  }

  def apply[ReceivableMessage, SendableMessage, Execution <: ExecutionMarshaling[Location], Location](remoteHostAddr: InetAddress, remotePort: Int): SocketObjectConnection[ReceivableMessage, SendableMessage] = apply[ReceivableMessage, SendableMessage, Execution, Location](new InetSocketAddress(remoteHostAddr, remotePort))

  def apply[ReceivableMessage, SendableMessage, Execution <: ExecutionMarshaling[Location], Location](remoteHostname: String, remotePort: Int): SocketObjectConnection[ReceivableMessage, SendableMessage] = apply[ReceivableMessage, SendableMessage, Execution, Location](new InetSocketAddress(remoteHostname, remotePort))

  protected def newSocket() = new SocketWithWriteTimeout(RuntimeConnection.socketWriteTimeout)

}

/** Interface to DOrcExecution that RuntimeConnection uses.  Provides
  * marshal/unmarshal methods and an execution ID.
  *
  * @author jthywiss
  * @tparam Location  Location type for origin/destination location references
  */
trait ExecutionMarshaling[Location] {
  type ExecutionId
  val executionId: ExecutionId
  val marshalExecutionObject: PartialFunction[(Location, AnyRef), AnyRef]
  val unmarshalExecutionObject: PartialFunction[(Location, AnyRef), AnyRef]
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
class ExecutionContextSerializationMarker[ExecutionId](val executionId: ExecutionId) extends Serializable

/** Deserializes Orc/dOrc values previously written using an
  * RuntimeConnectionOutputStream.  Extends ObjectInputStream to
  * provide extra serialization support for Orc/dOrc values.
  *
  * @author jthywiss
  */
protected class RuntimeConnectionInputStream[Execution <: ExecutionMarshaling[Location], Location, I <: Execution#ExecutionId](in: InputStream) extends ObjectInputStream(in) {

  enableResolveObject(true)

  @throws(classOf[IOException])
  override protected def resolveObject(obj: AnyRef): AnyRef = {
    obj match {
      case xm: ExecutionContextSerializationMarker[I] => {
        Logger.Marshal.finest(s"ExecutionContext ${orc.util.GetScalaTypeName(obj)}=$obj, xid=${xm.executionId}")
        currExecution = Some((currExecutionLookup.get)(xm.executionId))
        obj
      }
      case _ if currExecution.isDefined && currOrigin.isDefined && currExecution.get.unmarshalExecutionObject.isDefinedAt((currOrigin.get, obj)) =>
        currExecution.get.unmarshalExecutionObject((currOrigin.get, obj))
      //case _ if currExecution.isDefined /*&& currExecution.get.unmarshalValueWouldReplace(obj)*/ => currExecution.get.unmarshalValue(obj)
      case _ => super.resolveObject(obj)
    }
  }

  protected var currExecutionLookup: Option[(Execution#ExecutionId) => Execution] = None
  protected var currOrigin: Option[Location] = None
  protected var currExecution: Option[Execution] = None

  def setContext(executionLookup: (Execution#ExecutionId) => Execution, origin: Location): Unit = {
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
protected class RuntimeConnectionOutputStream[Execution <: ExecutionMarshaling[Location], Location, I <: Execution#ExecutionId](out: OutputStream) extends ObjectOutputStream(out) {

  enableReplaceObject(true)

  @throws(classOf[IOException])
  override protected def replaceObject(obj: AnyRef): AnyRef = {
    //Logger.Marshal.entering(getClass.getName, "replaceObject", Seq(s"${if (obj != null) obj.toString.take(60) else "null"}: ${orc.util.GetScalaTypeName(obj)}"))
    val result = try {
      obj match {
        case xm: ExecutionContextSerializationMarker[I] => {
          assert(xm.executionId == currExecution.get.executionId)
          obj
        }
        case _ if currExecution.isDefined && currDestination.isDefined && currExecution.get.marshalExecutionObject.isDefinedAt((currDestination.get, obj)) =>
          currExecution.get.marshalExecutionObject((currDestination.get, obj))
        case _ => super.replaceObject(obj)
      }
    } catch {
      /* Our caller, ObjectOutputStream, expects only IOExceptions.  Other
       * Throwables will cause it to abort and leave the stream in an
       * inconsistent state. Note: IOExceptions will be sent across the wire,
       * thrown at the receiver AND thrown at this sender.
       * SerializationReplacementFailureException will be caught, unwrapped
       * and rethrown at this sender, but rethrown as-is at the receiver. */
      case ioE: IOException => throw ioE
      case otherE: Throwable => throw new SerializationReplacementFailureException(obj.toString, otherE)
    }
    //Logger.Marshal.exiting(getClass.getName, "replaceObject", s"${if (result != null) result.toString.take(60) else "null"}: ${orc.util.GetScalaTypeName(result)}")
    result
  }

  protected var currExecution: Option[Execution] = None
  protected var currDestination: Option[Location] = None

  def setContext(execution: Execution, destination: Location): Unit = {
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

/** A wrapper to propagate non-IOException Throwables through Java's
  * ObjectStream machinery.
  */
class SerializationReplacementFailureException(objString: String, e: Throwable) extends IOException("Failure when replacing object for serialization, object: " + objString, e)

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
