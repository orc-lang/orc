//
// SocketObjectConnection.scala -- Scala class SocketObjectConnection, class ConnectionListener, and object ConnectionInitiator
// Project OrcScala
//
// Created by jthywiss on Nov 15, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ EOFException, IOException, ObjectInputStream, ObjectOutputStream }
import java.net.{ InetAddress, InetSocketAddress, ServerSocket, Socket, SocketException }

/** SocketObjectConnection, given an open Socket, provides Java object stream
  * send and receive operations over that socket.  The transmitted bytes are the
  * Java Object Serialization Stream encoding of the arguments.
  *
  * @author jthywiss
  */
class SocketObjectConnection[+ReceivableMessage, -SendableMessage](val socket: Socket) {

  SocketObjectConnectionLogger.entering(getClass.getName, "<init>", Seq(socket))

  /* Note: Always get output before input */
  val oos = new ObjectOutputStream(socket.getOutputStream())
  val ois = new ObjectInputStream(socket.getInputStream())

  /** Get an object from the stream. If the stream is open and no object is
    * available, block until one becomes available. If the stream is closed and
    * no object is available, throw.
    */
  def receive(): ReceivableMessage = ois synchronized {
    val obj = try {
      ois.readObject().asInstanceOf[ReceivableMessage]
    } catch {
      case e: SocketException if e.getMessage == "Socket closed" => throw new EOFException()
      case e: SocketException if e.getMessage == "Connection reset" => throw new EOFException()
    }
    SocketObjectConnectionLogger.finest(s"SocketObjectConnection.receive: Received $obj on $socket")
    obj
  }

  /** Put an object in the stream. If the stream is closed, throw.
    */
  def send(obj: SendableMessage) = oos synchronized {
    SocketObjectConnectionLogger.finest(s"SocketObjectConnection.send: Sending $obj on $socket")
    try {
      oos.writeObject(obj)
    } finally {
      oos.flush()
      /* Need to do a TCP push, but can't from Java */
    }
    //SocketObjectConnectionLogger.finest(s"SocketObjectConnection.send: Sent")
  }

  /** Close the stream and block until until all data is transmitted. Any
    * subsequent calls to send or receive will throw.
    *
    * When the stream is empty, return.
    */
  def close() {
    SocketObjectConnectionLogger.finer(s"SocketObjectConnection.close on $socket")
    try {
      oos.flush()
    } finally {
      try {
        oos.close()
      } finally {
        try {
          ois.close()
        } finally {
          socket.close()
        }
      }
    }
  }

  /** Close the stream immediately and return. Discard buffered
    * data. Any subsequent calls to send or receive will throw.
    */
  def abort() {
    SocketObjectConnectionLogger.finer(s"SocketObjectConnection.abort on $socket")
    socket synchronized { if (!closed) socket.setSoLinger(false, 0) }
    /* Intentionally not closing oos -- causes a flush */
    try {
      ois.close()
    } finally {
      socket.close()
    }
  }

  /** If the stream is currently closed, return true, otherwise return false.
    */
  def closed = socket.isClosed

  SocketObjectConnectionLogger.finer(s"SocketObjectConnection created on $socket")

}

object SocketObjectConnection {
  //  val readTimeout = 0 // milliseconds, 0 = infinite
  val closeTimeout = 10 // seconds
  //  val connectTimeout = 10 // seconds

  def configSocket(socket: Socket) {
    socket.setTcpNoDelay(true)
    socket.setKeepAlive(true)
    /* Socket's PerformancePreferences hasn't been
     * implemented yet, but set it for future use. */
    socket.setPerformancePreferences(1, 2, 0)
    socket.setTrafficClass(0xA0) /* DSCP CS5 */
    socket.setSoLinger(true, closeTimeout)
    //    socket.setSoTimeout(readTimeout)
  }

}

/** ConnectionListener listens on the given TCP port number for incoming
  * connections (a TCP "passive open"). Accepted connections result in new
  * SocketObjectConnection being returned for use in communicating with the peer.
  *
  * @author jthywiss
  */
class ConnectionListener[+ReceivableMessage, -SendableMessage](bindSockAddr: InetSocketAddress) {

  val serverSocket = newServerSocket()
  try {
    serverSocket.bind(bindSockAddr)
  } catch {
    case ioe: IOException => throw new IOException(s"Unable to bind to $bindSockAddr: ${ioe.getMessage}", ioe)
  }

  protected def newServerSocket() = new ServerSocket()

  def acceptConnection() = {
    val acceptedSocket = serverSocket.accept()
    SocketObjectConnectionLogger.finer(s"ConnectionListener accepted $acceptedSocket")
    SocketObjectConnection.configSocket(acceptedSocket)
    new SocketObjectConnection[ReceivableMessage, SendableMessage](acceptedSocket)
  }

  def close() = serverSocket.close()

  SocketObjectConnectionLogger.finer(s"ConnectionListener socket $serverSocket")

}

/** ConnectionInitiator actively opens a TCP connection to the given socket
  * (hostname-port pair).  Upon connection, a SocketObjectConnection is returned
  * to use for communicating with the peer.
  *
  * @author jthywiss
  */
object ConnectionInitiator {

  def apply[ReceivableMessage, SendableMessage](remoteSockAddr: InetSocketAddress, localSockAddr: InetSocketAddress = null) = {
    try {
      val socket = newSocket()
      SocketObjectConnection.configSocket(socket)
      if (localSockAddr != null) {
        socket.bind(localSockAddr)
      }
      socket.connect(remoteSockAddr)
      SocketObjectConnectionLogger.finer(s"ConnectionInitiator socket $socket")
      new SocketObjectConnection[ReceivableMessage, SendableMessage](socket)
    } catch {
      case ioe: IOException => throw new IOException(s"Unable to connect to $remoteSockAddr: ${ioe.getMessage}", ioe)
    }
  }

  def apply[ReceivableMessage, SendableMessage](remoteHostAddr: InetAddress, remotePort: Int): SocketObjectConnection[ReceivableMessage, SendableMessage] = apply[ReceivableMessage, SendableMessage](new InetSocketAddress(remoteHostAddr, remotePort))

  def apply[ReceivableMessage, SendableMessage](remoteHostname: String, remotePort: Int): SocketObjectConnection[ReceivableMessage, SendableMessage] = apply[ReceivableMessage, SendableMessage](new InetSocketAddress(remoteHostname, remotePort))

  protected def newSocket() = new Socket()

}

private object SocketObjectConnectionLogger extends orc.util.Logger("orc.util.SocketObjectConnection")
