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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

import orc.FutureReader
import orc.run.porce.runtime.Future

/** A reference to an Future value at another Location.
  *
  * @author jthywiss
  */
class RemoteFutureRef(futureManager: RemoteFutureManager, override val remoteRefId: RemoteFutureRef#RemoteRefId) extends Future() with RemoteRef {

  override def toString: String = f"${getClass.getName}(remoteRefId=$remoteRefId%#x)"

  /** Resolve this to a value and call publish and halt on each blocked FutureReader.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  override def bind(v: AnyRef) = {
    futureManager.sendFutureResolution(remoteRefId, Some(v))
  }

  /** Resolve this to stop and call halt on each blocked FutureReader.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  override def stop(): Unit = {
    futureManager.sendFutureResolution(remoteRefId, None)
  }

  futureManager.sendReadFuture(remoteRefId)
}

/** A remote reader that is blocked awaiting a local Future value.
  *
  * @author jthywiss
  */
class RemoteFutureReader(val fut: Future, val futureManager: RemoteFutureManager, futureId: RemoteFutureManager#RemoteFutureId) extends FutureReader {

  protected var readerLocations = new java.util.HashSet[PeerLocation](4)

  override def toString: String = f"${getClass.getName}(fut=$fut, futureManager=$futureManager, futureId=$futureId%#x)"

  def addReader(l: PeerLocation): Unit = synchronized {
    readerLocations.add(l)
    if (readerLocations.size == 1) {
      fut.read(this)
    }
  }

  protected def getAndClearReaders(): Array[PeerLocation] = synchronized {
    import scala.collection.JavaConverters._
    val readers = readerLocations.asScala.toArray
    readerLocations.clear()
    readers
  }

  override def publish(v: AnyRef): Unit = synchronized {
    Logger.Futures.entering(getClass.getName, s"$this publish $v")
    futureManager.sendFutureResult(getAndClearReaders(), futureId, fut, Some(v))
  }

  override def halt(): Unit = synchronized {
    Logger.Futures.entering(getClass.getName, s"$this halt")
    futureManager.sendFutureResult(getAndClearReaders(), futureId, fut, None)
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
  protected val servingLocalFutures = new java.util.concurrent.ConcurrentHashMap[Future, RemoteFutureId]
  // TODO: MEMORYLEAK: servingRemoteFutures is not cleaned as futures are no longer needed. This is because the future Id
  //       could be requested from another node with any amount of delay. This will be a problem as programs run longer or
  //       use more futures. The initial symptom is likely to be programs slowing down as they run, instead of a visible
  //       increase in the heap size. The object are small, but ConcurrentHashMap does show quite a bit of slowdown as the
  //       number of entries increases.
  protected val servingRemoteFutures = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureReader]
  protected val servingGroupsFuturesUpdateLock = new Object()

  protected val waitingReaders = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureRef]
  protected val waitingReadersUpdateLock = new Object()

  // TODO: PERFORMANCE: We are using locks here when we could probably use computeIfAbsent and fiends.

  def ensureFutureIsRemotelyAccessibleAndGetId(fut: Future): RemoteFutureId = {
    //Logger.Futures.entering(getClass.getName, "ensureFutureIsRemotelyAccessibleAndGetId")
    fut match {
      case rfut: RemoteFutureRef => rfut.remoteRefId
      case _ => servingGroupsFuturesUpdateLock synchronized {
        if (servingLocalFutures.contains(fut)) {
          servingLocalFutures.get(fut)
        } else {
          val newFutureId = execution.freshRemoteRefId()
          val newReader = new RemoteFutureReader(fut, execution, newFutureId)
          servingLocalFutures.put(fut, newFutureId)
          servingRemoteFutures.put(newFutureId, newReader)
          newFutureId
        }
      }
    }
  }

  def futureForId(futureId: RemoteFutureId): Future = {
    if (execution.homeLocationForRemoteRef(futureId) == execution.runtime.asInstanceOf[DOrcRuntime].here) {
      servingRemoteFutures.get(futureId).fut
    } else {
      waitingReadersUpdateLock synchronized {
        if (!waitingReaders.contains(futureId)) {
          val newFuture = new RemoteFutureRef(execution, futureId)
          waitingReaders.putIfAbsent(futureId, newFuture)
        }
      }
      waitingReaders.get(futureId)
    }
  }

  def sendReadFuture(futureId: RemoteFutureId): Unit = {
    val homeLocation = execution.homeLocationForRemoteRef(futureId)
    Tracer.traceFutureReadSend(futureId, execution.runtime.here, homeLocation)
    homeLocation.sendInContext(execution)(ReadFutureCmd(executionId, futureId, followerExecutionNum))
  }

  def readFuture(futureId: RemoteFutureId, readerFollowerNum: DOrcRuntime#RuntimeId): Unit = {
    Logger.Futures.fine(f"Posting read on $futureId%#x, with reader at follower number $readerFollowerNum")
    servingRemoteFutures.get(futureId).addReader(execution.locationForFollowerNum(readerFollowerNum))
  }

  def sendFutureResult(readers: Traversable[PeerLocation], futureId: RemoteFutureId, fut: Future, value: Option[AnyRef]): Unit = {
    Logger.Futures.entering(getClass.getName, "sendFutureResult", Seq(readers, "0x" + futureId.toHexString, value))
    readers foreach { reader =>
      val mv = value.map(execution.marshalValue(reader)(_))
      Tracer.traceFutureResultSend(futureId, execution.runtime.here, reader)
      reader.sendInContext(execution)(DeliverFutureResultCmd(execution.executionId, futureId, mv))
    }
    servingLocalFutures.remove(fut)
    // TODO: PERFORMANCE: See servingRemoteFutures declaration
    //servingRemoteFutures.remove(futureId)
  }

  def deliverFutureResult(origin: PeerLocation, futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    val reader = waitingReaders.get(futureId)
    if (reader != null) {
      value match {
        case Some(v) => {
          Logger.Futures.fine(f"Future $futureId%#x was resolved to $v")
          reader.localBind(execution.unmarshalValue(origin)(v))
        }
        case None => {
          Logger.Futures.fine(f"Future $futureId%#x was resolved to stop")
          reader.localStop()
        }
      }
      waitingReaders.remove(futureId)
    } else {
      Logger.Futures.finer(f"deliverFutureResult reader not found, id=$futureId%#x")
    }
  }

  def sendFutureResolution(futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    val homeLocation = execution.homeLocationForRemoteRef(futureId)
    val marshaledFutureValue = value match {
      case Some(v) => Some(execution.marshalValue(homeLocation)(v))
      case None => value
    }
    Tracer.traceFutureResolveSend(futureId, execution.runtime.here, homeLocation)
    homeLocation.sendInContext(execution)(ResolveFutureCmd(executionId, futureId, marshaledFutureValue))
  }

  def receiveFutureResolution(origin: PeerLocation, futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    value match {
      case Some(v) => servingRemoteFutures.get(futureId).fut.bind(execution.unmarshalValue(origin)(v))
      case None => servingRemoteFutures.get(futureId).fut.stop()
    }
  }

}
