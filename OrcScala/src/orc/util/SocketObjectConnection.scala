//
// SocketObjectConnection.scala -- Scala class SocketObjectConnection, class ConnectionListener, and object ConnectionInitiator
// Project OrcScala
//
// Created by jthywiss on Nov 15, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
class SocketObjectConnection[+R, -S](val socket: Socket) {

  SocketObjectConnectionLogger.entering(getClass.getName, "<init>", Seq(socket))

  /* Note: Always get output before input */
  val oos = new ObjectOutputStream(socket.getOutputStream())
  val ois = new ObjectInputStream(socket.getInputStream())

  /** Get an object from the stream. If the stream is open and no object is
    * available, block until one becomes available. If the stream is closed and
    * no object is available, throw.
    */
  def receive(): R = ois synchronized {
    val obj = try {
      ois.readObject().asInstanceOf[R]
    } catch {
      case e: SocketException if e.getMessage == "Socket closed" => throw new EOFException()
      case e: SocketException if e.getMessage == "Connection reset" => throw new EOFException()
    }
    SocketObjectConnectionLogger.finest(s"SocketObjectConnection.receive: Received $obj on $socket")
    obj
  }

  /** Put an object in the stream. If the stream is closed, throw.
    */
  def send(obj: S) = oos synchronized {
    SocketObjectConnectionLogger.finest(s"SocketObjectConnection.send: Sending $obj on $socket")
    oos.writeObject(obj)
    oos.flush()
    //SocketObjectConnectionLogger.finest(s"SocketObjectConnection.send: Sent")
    /* Need to do a TCP push, but can't from Java */
  }

  /** Close the stream and block until until all data is transmitted. Any
    * subsequent calls to send or receive will throw.
    *
    * When the stream is empty, return.
    */
  def close() {
    SocketObjectConnectionLogger.finer(s"SocketObjectConnection.close on $socket")
    oos.flush()
    oos.close()
    ois.close()
    socket.close()
  }

  /** Close the stream immediately and return. Discard buffered
    * data. Any subsequent calls to send or receive will throw.
    */
  def abort() {
    SocketObjectConnectionLogger.finer(s"SocketObjectConnection.abort on $socket")
    socket synchronized { if (!closed) socket.setSoLinger(false, 0) }
    /* Intentionally not closing oos -- causes a flush */
    ois.close()
    socket.close()
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
    val IPTOS_LOWDELAY = 0x10

    socket.setTcpNoDelay(true)
    socket.setKeepAlive(true)
    /* Socket's PerformancePreferences hasn't been
     * implemented yet, but set is for future use. */
    socket.setPerformancePreferences(1, 2, 0)
    socket.setTrafficClass(IPTOS_LOWDELAY)
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
class ConnectionListener[+R, -S](bindSockAddr: InetSocketAddress) {

  val serverSocket = new ServerSocket()
  try {
    serverSocket.bind(bindSockAddr)
  } catch {
    case ioe: IOException => throw new IOException(s"Unable to bind to $bindSockAddr: ${ioe.getMessage}", ioe)
  }

  def acceptConnection() = {
    val acceptedSocket = serverSocket.accept()
    SocketObjectConnectionLogger.finer(s"ConnectionListener accepted $acceptedSocket")
    SocketObjectConnection.configSocket(acceptedSocket)
    new SocketObjectConnection[R, S](acceptedSocket)
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

  def apply[R, S](remoteSockAddr: InetSocketAddress, localSockAddr: InetSocketAddress = null) = {
    try {
      val socket = new Socket()
      SocketObjectConnection.configSocket(socket)
      if (localSockAddr != null) {
        socket.bind(localSockAddr)
      }
      socket.connect(remoteSockAddr)
      SocketObjectConnectionLogger.finer(s"ConnectionInitiator socket $socket")
      new SocketObjectConnection[R, S](socket)
    } catch {
      case ioe: IOException => throw new IOException(s"Unable to connect to $remoteSockAddr: ${ioe.getMessage}", ioe)
    }
  }

  def apply[R, S](remoteHostAddr: InetAddress, remotePort: Int): SocketObjectConnection[R, S] = apply[R, S](new InetSocketAddress(remoteHostAddr, remotePort))

  def apply[R, S](remoteHostname: String, remotePort: Int): SocketObjectConnection[R, S] = apply[R, S](new InetSocketAddress(remoteHostname, remotePort))

}

private object SocketObjectConnectionLogger extends orc.util.Logger("orc.lib.SocketObjectConnection")
