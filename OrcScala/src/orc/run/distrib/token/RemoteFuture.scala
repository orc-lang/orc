//
// RemoteFuture.scala -- Scala classes RemoteFuture and RemoteFutureReader, and trait RemoteFutureManager
// Project OrcScala
//
// Created by jthywiss on Jan 13, 2016.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import java.io.IOException

import orc.error.OrcException
import orc.run.core.{ Blockable, Blocker, Future, LocalFuture }
import orc.run.distrib.Logger
import orc.run.distrib.common.RemoteRef

/** A reference to an GraftGroup value at another Location.
  *
  * @author jthywiss
  */
class RemoteFutureRef(execution: DOrcExecution, override val remoteRefId: RemoteRef#RemoteRefId) extends LocalFuture(execution.runtime) with RemoteRef {

  override def toString: String = super.toString + f"(remoteRefId=$remoteRefId%#x)"

  execution.sendReadFuture(remoteRefId)
}

/** A remote reader that is blocked awaiting a local Future value.
  *
  * @author jthywiss
  */
class RemoteFutureReader(val fut: Future, val futureManager: RemoteFutureManager, futureId: RemoteFutureRef#RemoteRefId) extends Blockable {

  protected val readerLocations = new scala.collection.mutable.HashSet[PeerLocation]()

  override def toString: String = f"${getClass.getName}(fut=$fut, futureManager=$futureManager, futureId=$futureId%#x)"

  def addReader(l: PeerLocation): Unit = synchronized {
    readerLocations += l
    if (readerLocations.size == 1) {
      fut.read(this)
    }
  }

  protected def getAndClearReaders(): List[PeerLocation] = synchronized {
    val readers = readerLocations.toList
    readerLocations.clear()
    readers
  }

  override def awakeNonterminalValue(v: AnyRef): Unit = synchronized {
    throw new AssertionError("awakeNonterminalValue called on RemoteFutureReader (This is an interpreter bug).")
  }

  override def awakeTerminalValue(v: AnyRef): Unit = synchronized {
    //Logger.Futures.entering(getClass.getName, "awakeNonterminalValue")
    futureManager.sendFutureResult(getAndClearReaders(), futureId, Some(v))
  }

  override def awakeStop(): Unit = synchronized {
    //Logger.Futures.entering(getClass.getName, "awakeStop")
    futureManager.sendFutureResult(getAndClearReaders(), futureId, None)
  }

  override def awakeException(e: OrcException): Unit = throw new AssertionError(s"RemoteFutureReader.awakeException($e) called; Futures don't call this on their blockables")

  override def blockOn(b: Blocker): Unit = {}

  override def setQuiescent(): Unit = throw new AssertionError("RemoteFutureReader.setQuiescent() called; Futures don't call this on their blockables")

  override def unsetQuiescent(): Unit = throw new AssertionError("RemoteFutureReader.unsetQuiescent() called; Futures don't call this on their blockables")

  override def run(): Unit = {
    //Logger.Futures.entering(getClass.getName, "run")
    fut.check(this)
  }

}

/** A mix-in to manage remote futures.
  *
  * @author jthywiss
  */
trait RemoteFutureManager {
  execution: DOrcExecution =>

  type RemoteFutureId = RemoteFutureRef#RemoteRefId

  // These two maps are inverses of each other (sorta)
  /** Map from a local ("real") future to its assigned RemoteFutureId. */
  protected val servingLocalFutures = new java.util.concurrent.ConcurrentHashMap[Future, RemoteFutureId]
  /** Map from a RemoteFutureId for a local future to its local proxy for the remote readers. */
  protected val servingRemoteFutures = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureReader]
  protected val servingGroupsFuturesUpdateLock = new Object()

  /** Map from a RemoteFutureId for a remote future to its local proxy
    * (RemoteFutureRef), which local FutureReaders block on.
    */
  protected val waitingReaders = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureRef]
  protected val waitingReadersUpdateLock = new Object()

  /** Given a future (local or remote), get its RemoteFutureId.  If the
    * future is a local future that hasn't been exposed as a remote future
    * previously, set it up to be remotely ref'ed, and return its new ID.
    */
  def ensureFutureIsRemotelyAccessibleAndGetId(fut: Future): RemoteFutureId = {
    //Logger.Futures.entering(getClass.getName, "ensureFutureIsRemotelyAccessibleAndGetId")
    fut match {
      case rfut: RemoteFutureRef => rfut.remoteRefId
      case _ => servingGroupsFuturesUpdateLock synchronized {
        if (servingLocalFutures.contains(fut)) {
          servingLocalFutures.get(fut)
        } else {
          val newFutureId = freshRemoteRefId()
          val newReader = new RemoteFutureReader(fut, this, newFutureId)
          servingLocalFutures.put(fut, newFutureId)
          servingRemoteFutures.put(newFutureId, newReader)
          newFutureId
        }
      }
    }
  }

  /** Get the future for the given ID.  If the ID refers to a future at this
    * location, that future is returned.  Otherwise, a RemoteFutureRef for
    * the future is returned.
    */
  def futureForId(futureId: RemoteFutureId): Future = {
    if (homeLocationForRemoteRef(futureId) == runtime.asInstanceOf[DOrcRuntime].here) {
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

  /** Send request to be sent the resolution of a remote future. */
  def sendReadFuture(futureId: RemoteFutureId): Unit = {
    val homeLocation = homeLocationForRemoteRef(futureId)
    Tracer.traceFutureReadSend(futureId, execution.runtime.here, homeLocation)
    homeLocation.sendInContext(execution)(ReadFutureCmd(executionId, futureId, execution.runtime.runtimeId))
  }

  /** Record remote request to be sent the resolution of a future we're serving. */
  def readFuture(futureId: RemoteFutureId, readerFollowerRuntimeId: DOrcRuntime.RuntimeId): Unit = {
    servingRemoteFutures.get(futureId).addReader(runtime.locationForRuntimeId(readerFollowerRuntimeId))
  }

  /** Send the resolution of a future we're serving. */
  def sendFutureResult(readers: Traversable[PeerLocation], futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    Logger.Futures.entering(getClass.getName, "sendFutureResult", Seq(readers, "0x" + futureId.toHexString, value))
    readers foreach { reader =>
      val mv = value.map(execution.marshalValue(reader)(_))
      Tracer.traceFutureResultSend(futureId, execution.runtime.here, reader)
      try {
        reader.sendInContext(execution)(DeliverFutureResultCmd(executionId, futureId, mv))
      } catch {
        case e: IOException if !execution.currentlyActiveLocation(reader) =>
          /* Disregard send failures to closed destinations */
      }
    }
  }

  /** Locally deliver the resolution of a remote future. */
  def deliverFutureResult(origin: PeerLocation, futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    val reader = waitingReaders.get(futureId)
    if (reader != null) {
      value match {
        case Some(v) => reader.bind(execution.unmarshalValue(origin)(v))
        case None => reader.stop()
      }
    } else {
      Logger.Futures.finer(s"deliverFutureResult reader not found, id=$futureId")
    }
  }
}
