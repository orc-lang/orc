//
// RemoteFuture.scala -- Scala classes RemoteFuture and RemoteFutureReader, and trait RemoteFutureManager
// Project OrcScala
//
// Created by jthywiss on Jan 13, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.error.OrcException
import orc.run.core.{ Blockable, Blocker, LateBindGroup, RightSidePublished, RightSideUnknown }

/** A reference to an LateBindGroup value at another Location.
  *
  * @author jthywiss
  */
class RemoteFutureRef(execution: DOrcExecution, override val remoteRefId: RemoteFutureRef#RemoteRefId) extends LateBindGroup(execution) with RemoteRef {
  override type RemoteRefId = Long

  execution.sendReadFuture(remoteRefId)
  execution.remove(this) // Don't hold the execution open simply because this ref exists

  def onResult(v: Option[AnyRef]) = synchronized {
    Logger.entering(getClass.getName, "onResult")
    state match {
      case RightSideUnknown(waitlist) => {
        state = RightSidePublished(v)
        for (w <- waitlist) { runtime.schedule(w) }
      }
      case _ => {}
    }
  }

}

/** A remote reader that is blocked awaiting a local LateBindGroup value.
  *
  * @author jthywiss
  */
class RemoteFutureReader(g: LateBindGroup, futureId: RemoteFutureRef#RemoteRefId) extends Blockable {

  protected val readerLocations = new scala.collection.mutable.HashSet[Location]()

  def addReader(l: Location) = synchronized {
    readerLocations += l
    if (readerLocations.size == 1) {
      g.read(this)
    }
  }

  protected def getAndClearReaders() = synchronized {
    val readers = readerLocations.toList
    readerLocations.clear()
    readers
  }

  override def awakeValue(v: AnyRef) = synchronized {
    Logger.entering(getClass.getName, "awakeValue")
    val dorcExecution = g.execution.asInstanceOf[DOrcExecution]
    dorcExecution.sendFutureResult(getAndClearReaders(), futureId, Some(v))
  }

  override def awakeStop() = synchronized {
    Logger.entering(getClass.getName, "awakeStop")
    val dorcExecution = g.execution.asInstanceOf[DOrcExecution]
    dorcExecution.sendFutureResult(getAndClearReaders(), futureId, None)
  }

  override def awakeException(e: OrcException) = throw new AssertionError(s"RemoteFutureReader.awakeException($e) called; LateBindGroups don't call this on their blockables")

  override def blockOn(b: Blocker) = {}

  override def setQuiescent(): Unit = throw new AssertionError("RemoteFutureReader.setQuiescent() called; LateBindGroups don't call this on their blockables")

  override def unsetQuiescent(): Unit = throw new AssertionError("RemoteFutureReader.unsetQuiescent() called; LateBindGroups don't call this on their blockables")

  override def run() {
    Logger.entering(getClass.getName, "run")
    g.check(this)
  }

}

/** A mix-in to manage remote futures.
  *
  * @author jthywiss
  */
trait RemoteFutureManager { self: DOrcExecution =>

  // These two maps are inverses of each other (sorta)
  protected val servingGroups = new java.util.concurrent.ConcurrentHashMap[LateBindGroup, RemoteFutureRef#RemoteRefId]
  protected val servingFutures = new java.util.concurrent.ConcurrentHashMap[RemoteFutureRef#RemoteRefId, RemoteFutureReader]
  protected val servingGroupsFuturesUpdateLock = new Object()

  protected val waitingReaders = new java.util.concurrent.ConcurrentHashMap[RemoteFutureRef#RemoteRefId, RemoteFutureRef]
  protected val waitingReadersUpdateLock = new Object()

  def ensureFutureIsRemotelyAccessibleAndGetId(g: LateBindGroup) = {
    Logger.entering(getClass.getName, "ensureFutureIsRemotelyAccessibleAndGetId")
    g match {
      case rg: RemoteFutureRef => rg.remoteRefId
      case _ => servingGroupsFuturesUpdateLock synchronized {
        if (servingGroups.contains(g)) {
          servingGroups.get(g)
        } else {
          val newFutureId = freshRemoteFutureId()
          val newReader = new RemoteFutureReader(g, newFutureId)
          servingGroups.put(g, newFutureId)
          servingFutures.put(newFutureId, newReader)
          newFutureId
        }
      }
    }
  }

  def futureForId(futureId: RemoteFutureRef#RemoteRefId) = {
    waitingReadersUpdateLock synchronized {
      if (!waitingReaders.contains(futureId)) {
        val newFuture = new RemoteFutureRef(this, futureId)
        waitingReaders.putIfAbsent(futureId, newFuture)
      }
    }
    waitingReaders.get(futureId)
  }

  def sendReadFuture(futureId: RemoteFutureRef#RemoteRefId) {
    val homeLocation = homeLocationForRemoteFutureId(futureId)
    homeLocation.send(ReadFutureCmd(executionId, futureId, followerExecutionNum))
  }

  def readFuture(futureId: RemoteFutureRef#RemoteRefId, readerFollowerNum: Int) {
    servingFutures.get(futureId).addReader(locationForFollowerNum(readerFollowerNum))
  }

  def sendFutureResult(readers: Traversable[Location], futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) {
    Logger.entering(getClass.getName, "sendFutureResult")
    readers foreach { _.send(DeliverFutureResultCmd(executionId, futureId, value)) }
  }

  def deliverFutureResult(futureId: RemoteFutureRef#RemoteRefId, value: Option[AnyRef]) {
    val reader = waitingReaders.get(futureId)
    if (reader != null) {
      reader.onResult(value)
    } else {
      Logger.finer(s"deliverFutureResult reader not found, id=$futureId")
    }
  }
}
