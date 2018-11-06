//
// RemoteFuture.scala -- Scala classes RemoteFuture and RemoteFutureReader, and trait RemoteFutureManager
// Project PorcE
//
// Created by jthywiss on Jan 13, 2016.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.FutureReader
import orc.run.porce.runtime.Future

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/** A reference to an Future value at another Location.
  *
  * @author jthywiss
  */
class RemoteFutureRef(futureManager: RemoteFutureManager, override val remoteRefId: RemoteFutureRef#RemoteRefId, raceFreeResolution: Boolean) extends Future(raceFreeResolution) with RemoteRef {

  override def toString: String = f"${getClass.getName}(remoteRefId=$remoteRefId%#x,cachedState=${get},numWaiters=$numWaiters)"

  /** Resolve this to a value and call publish and halt on each blocked FutureReader.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  override def bind(v: AnyRef) = {
    if (raceFreeResolution) {
      Logger.Futures.finest("Future $futureId%#x: Shortcutting bind communication, since raceFreeResolution=true")
      localBind(v)
    }
    futureManager.sendFutureResolution(remoteRefId, Some(v))
  }

  /** Resolve this to stop and call halt on each blocked FutureReader.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  override def stop(): Unit = {
    if (raceFreeResolution) {
      Logger.Futures.finest("Future $futureId%#x: Shortcutting bind communication, since raceFreeResolution=true")
      localStop()
    }
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
    Logger.Futures.entering(getClass.getName, "publish", Seq(s"$this publish $v"))
    futureManager.sendFutureResult(getAndClearReaders(), futureId, fut, Some(v))
  }

  override def halt(): Unit = synchronized {
    Logger.Futures.entering(getClass.getName, "halt", Seq(s"$this halt"))
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
  // TODO: Determine when a served RemoteFutureId is no longer referenced, and remove entries from these maps.
  /** Map from a local ("real") future to its assigned RemoteFutureId. */
  protected val servingLocalFutures = new java.util.concurrent.ConcurrentHashMap[Future, RemoteFutureId]
  /** Map from a RemoteFutureId for a local future to its local proxy for the remote readers. */
  protected val servingRemoteFutures = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureReader]

  /** Map from a RemoteFutureId for a remote future to its local proxy
    * (RemoteFutureRef), which local FutureReaders block on.
    */
  protected val waitingReaders = new java.util.concurrent.ConcurrentHashMap[RemoteFutureId, RemoteFutureRef]

  /** Given a future (local or remote), get its RemoteFutureId.  If the
    * future is a local future that hasn't been exposed as a remote future
    * previously, set it up to be remotely ref'ed, and return its new ID.
    */
  def ensureFutureIsRemotelyAccessibleAndGetId(fut: Future): RemoteFutureId = {
    //Logger.Futures.entering(getClass.getName, "ensureFutureIsRemotelyAccessibleAndGetId", Seq(fut))
    fut match {
      case rfut: RemoteFutureRef => rfut.remoteRefId
      case _ => servingLocalFutures.computeIfAbsent(fut, fut => {
        val newFutureId = execution.freshRemoteRefId()
        val newReader = new RemoteFutureReader(fut, execution, newFutureId)
        servingRemoteFutures.put(newFutureId, newReader)
        newFutureId
      })
    }
  }

  /** Get the future for the given ID.  If the ID refers to a future at this
    * location, that future is returned.  Otherwise, a RemoteFutureRef for
    * the future is returned.
    */
  def futureForId(futureId: RemoteFutureId, raceFreeResolution: Boolean): Future = {
    if (execution.homeLocationForRemoteRef(futureId) == execution.runtime.asInstanceOf[DOrcRuntime].here) {
      servingRemoteFutures.get(futureId).fut ensuring (_.raceFreeResolution == raceFreeResolution)
    } else {
      waitingReaders.computeIfAbsent(futureId, new RemoteFutureRef(execution, _, raceFreeResolution))
    }
  }

  /** Send request to be sent the resolution of a remote future. */
  def sendReadFuture(futureId: RemoteFutureId): Unit = {
    val homeLocation = execution.homeLocationForRemoteRef(futureId)
    Tracer.traceFutureReadSend(futureId, execution.runtime.here, homeLocation)
    homeLocation.sendInContext(execution)(ReadFutureCmd(executionId, futureId, followerExecutionNum))
  }

  /** Record remote request to be sent the resolution of a future we're serving. */
  def readFuture(futureId: RemoteFutureId, readerFollowerNum: DOrcRuntime#RuntimeId): Unit = {
    Logger.Futures.fine(f"Posting read on $futureId%#x, with reader at follower number $readerFollowerNum")
    servingRemoteFutures.get(futureId).addReader(execution.locationForFollowerNum(readerFollowerNum))
  }

  /** Send the resolution of a future we're serving. */
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

  /** Locally deliver the resolution of a remote future. */
  def deliverFutureResult(origin: PeerLocation, futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    val reader = waitingReaders.get(futureId)
    if (reader != null) {
      value match {
        case Some(v) => {
          val unmarshalledValue = execution.unmarshalValue(origin)(v)
          Logger.Futures.fine(f"Future $futureId%#x (reader $reader) was resolved to $unmarshalledValue")
          reader.localBind(unmarshalledValue)
        }
        case None => {
          Logger.Futures.fine(f"Future $futureId%#x (reader $reader) was resolved to stop")
          reader.localStop()
        }
      }
      waitingReaders.remove(futureId)
    } else {
      Logger.Futures.finer(f"deliverFutureResult reader not found, id=$futureId%#x")
    }
  }

  /** Send our resolution of a remote future. */
  def sendFutureResolution(futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    val homeLocation = execution.homeLocationForRemoteRef(futureId)
    val marshaledFutureValue = value match {
      case Some(v) => Some(execution.marshalValue(homeLocation)(v))
      case None => value
    }
    Tracer.traceFutureResolveSend(futureId, execution.runtime.here, homeLocation)
    homeLocation.sendInContext(execution)(ResolveFutureCmd(executionId, futureId, marshaledFutureValue))
  }

  /** Handle remote resolution of a future we're serving. */
  def receiveFutureResolution(origin: PeerLocation, futureId: RemoteFutureId, value: Option[AnyRef]): Unit = {
    value match {
      case Some(v) => servingRemoteFutures.get(futureId).fut.bind(execution.unmarshalValue(origin)(v))
      case None => servingRemoteFutures.get(futureId).fut.stop()
    }
  }

}
