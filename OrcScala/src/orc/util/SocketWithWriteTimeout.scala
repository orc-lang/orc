//
// SocketWithWriteTimeout.scala -- Scala classes SocketWithWriteTimeout and ServerSocketWithWriteTimeout
// Project OrcScala
//
// Created by jthywiss on Mar 20, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ IOException, OutputStream }
import java.net.{ ServerSocket, Socket, SocketException }
import java.util.{ Timer, TimerTask }

/** A Socket that has a timeout on its write operations.  If a write does not
  * complete within the timeout period, the Socket is closed, and the write
  * operation throws a SocketException.
  *
  * @author jthywiss
  */
class SocketWithWriteTimeout(initialWriteTimeout: Long = 0L) extends Socket() {

  private var writeTimeout: Long = initialWriteTimeout

  private lazy val wrappedOutputStream = new SocketWithWriteTimeout.SocketOutputStreamWrapper(super.getOutputStream(), this)

  def getWriteTimeout: Long = synchronized { writeTimeout }

  def setWriteTimeout(writeTimeout: Long) = synchronized { this.writeTimeout = writeTimeout }

  @throws[IOException]
  override def getOutputStream: OutputStream = wrappedOutputStream

}

object SocketWithWriteTimeout {

  private lazy val timer = new Timer("Socket write Timer", true)

  /** An OutputStream wrapper that implements timeouts on write operations.
    *
    * @author jthywiss
    * @see SocketWithWriteTimeout
    */
  class SocketOutputStreamWrapper(os: OutputStream, socket: SocketWithWriteTimeout) extends OutputStream() {

    @throws[IOException]
    override def write(b: Int): Unit = {
      val timeoutTimerTask = beginWrite(socket.getWriteTimeout)
      try {
        os.write(b)
      } finally {
        endWrite(timeoutTimerTask)
      }
    }

    @throws[IOException]
    override def write(b: Array[Byte]): Unit = {
      val timeoutTimerTask = beginWrite(socket.getWriteTimeout)
      try {
        os.write(b)
      } finally {
        endWrite(timeoutTimerTask)
      }
    }

    @throws[IOException]
    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      val timeoutTimerTask = beginWrite(socket.getWriteTimeout)
      try {
        os.write(b, off, len)
      } finally {
        endWrite(timeoutTimerTask)
      }
    }

    @throws[IOException]
    override def flush(): Unit = os.flush()

    @throws[IOException]
    override def close(): Unit = os.close()

    protected def beginWrite(writeTimeout: Long) = {
      if (writeTimeout > 0) {
        val timeoutTimerTask = new SocketWriteTimerTask(socket)
        Logger.finest("Scheduling write timeout for " + socket.toString)
        timer.schedule(timeoutTimerTask, writeTimeout)
        Some(timeoutTimerTask)
      } else {
        None
      }
    }

    protected def endWrite(timeoutTimerTask: Option[TimerTask]) = {
      timeoutTimerTask.foreach( {
        Logger.finest("Canceling write timeout for " + socket.toString)
        _.cancel()
      })
    }

  }

  /** A TimerTask that closes a Socket.
    *
    * @author jthywiss
    * @see SocketOutputStreamWrapper
    */
  class SocketWriteTimerTask(socket: Socket) extends TimerTask() {
    override def run(): Unit = {
      Logger.warning("Write timeout on Socket, closing: " + socket.toString)
      socket.close()
    }
  }

  private object Logger extends orc.util.Logger("orc.util.SocketWithWriteTimeout")

}

/** A ServerSocket that returns SocketWithWriteTimeout instances from accept().
  *
  * @author jthywiss
  */
class ServerSocketWithWriteTimeout(initialTimeout: Long = 0L) extends ServerSocket() {

  private var timeout = initialTimeout

  def getTimeout(): Long = synchronized { timeout }

  def setTimeout(timeout: Long) = synchronized { this.timeout = timeout }

  @throws[IOException]
  override def accept(): Socket = {
    if (isClosed()) throw new SocketException("Socket is closed")
    if (!isBound()) throw new SocketException("Socket is not bound yet")
    val s = new SocketWithWriteTimeout(getTimeout())
    implAccept(s)
    s
  }

}
