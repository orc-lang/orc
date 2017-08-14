//
// RemoteFuture.scala -- Scala classes RemoteFuture and RemoteFutureReader, and trait RemoteFutureManager
// Project PorcE
//
// Created by jthywiss on Jan 13, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.{ Future, FutureReader }

/** A reference to an GraftGroup value at another Location.
  *
  * @author jthywiss
  */
class RemoteFutureRef(execution: DOrcExecution, override val remoteRefId: RemoteFutureRef#RemoteRefId) extends orc.run.porce.runtime.Future() with RemoteRef {
  override type RemoteRefId = Long

  override def toString = super.toString + f"(remoteRefId=$remoteRefId%#x)"

  execution.sendReadFuture(remoteRefId)
}

/** A remote reader that is blocked awaiting a local Future value.
  *
  * @author jthywiss
  */
class RemoteFutureReader(val fut: Future, val execution: DOrcExecution, futureId: RemoteFutureRef#RemoteRefId) extends FutureReader {

  protected val readerLocations = new scala.collection.mutable.HashSet[PeerLocation]()

  def addReader(l: PeerLocation) = synchronized {
    readerLocations += l
    if (readerLocations.size == 1) {
      fut.read(this)
    }
  }

  protected def getAndClearReaders() = synchronized {
    val readers = readerLocations.toList
    readerLocations.clear()
    readers
  }

  override def publish(v: AnyRef) = synchronized {
    //Logger.entering(getClass.getName, "publish")
    execution.sendFutureResult(getAndClearReaders(), futureId, Some(v))
  }

  override def halt() = synchronized {
    //Logger.entering(getClass.getName, "halt")
    execution.sendFutureResult(getAndClearReaders(), futureId, None)
  }

}

/** A mix-in to manage remote futures.
  *
  * @author jthywiss
  */
trait RemoteFutureManager {
  self: DOrcExecution =>

  // These two maps are inverses of each other (sorta)
  protected val servingLocalFutures = new java.util.concurrent.ConcurrentHashMap[Future, RemoteFutureRef#RemoteRefId]
  protected val servingRemoteFutures = new java.util.concurrent.ConcurrentHashMap[RemoteFutureRef#RemoteRefId, RemoteFutureReader]
  protected val servingGroupsFuturesUpdateLock = new Object()

  protected val waitingReaders = new java.util.concurrent.ConcurrentHashMap[RemoteFutureRef#RemoteRefId, RemoteFutureRef]
  protected val waitingReadersUpdateLock = new Object()

  def ensureFutureIsRemotelyAccessibleAndGetId(fut: Future) = {
    //Logger.entering(getClass.getName, "ensureFutureIsRemotelyAccessibleAndGetId")
    fut match {
      case rfut: RemoteFutureRef => rfut.remoteRefId
      case _ => servingGroupsFuturesUpdateLock synchronized {
        if (servingLocalFutures.contains(fut)) {
          servingLocalFutures.get(fut)
        } else {
          val newFutureId = freshRemoteFutureId()
          val newReader = new RemoteFutureReader(fut, this, newFutureId)
          servingLocalFutures.put(fut, newFutureId)
          servingRemoteFutures.put(newFutureId, newReader)
          newFutureId
        }
      }
    }
  }

  def futureForId(futureId: RemoteFutureRef#RemoteRefId) = {
    if (homeLocationForRemoteFutureId(futureId) == runtime.asInstanceOf[DOrcRuntime].here) {
      servingRemoteFutures.get(futureId).fut
    } else {
      waitingReadersUpdateLock synchronized {
        if (!waitingReaders.contains(futureId)) {
          val newFuture = new RemoteFutureRef(this, futureId)
          waitingReaders.putIfAbsent(futureId, newFuture)
        }
      }
      waitingReaders.get(futureId)
    }
  }

  def sendReadFuture(futureId: RemoteFutureRef#RemoteRefId) {
    val homeLocation = homeLocationForRemoteFutureId(futureId)
    Tracer.traceFutureReadSend(futureId, self.runtime.here, homeLocation)
    homeLocation.sendInContext(self)(ReadFutureCmd(executionId, futureId, followerExecutionNum))
  }

  def readFuture(futureId: RemoteFutureRef#RemoteRefId, readerFollowerNum: DOrcRuntime#RuntimeId) {
    servingRemoteFutures.get(futureId).addReader(locationForFollowerNum(readerFollowerNum))
  }

  def sendFutureResult(readers: Traversable[PeerLocation], futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) {
    Logger.entering(getClass.getName, "sendFutureResult", Seq(readers, "0x"+futureId.toHexString, value))
    readers foreach { reader =>
      val mv = value.map(self.marshalValue(reader)(_))
      Tracer.traceFutureResultSend(futureId, self.runtime.here, reader)
      reader.sendInContext(self)(DeliverFutureResultCmd(executionId, futureId, mv))
    }
  }

  def deliverFutureResult(futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) {
    val reader = waitingReaders.get(futureId)
    if (reader != null) {
      value match {
        case Some(v) => reader.bind(self.unmarshalValue(v))
        case None => reader.stop()
      }
    } else {
      Logger.finer(s"deliverFutureResult reader not found, id=$futureId")
    }
  }
}