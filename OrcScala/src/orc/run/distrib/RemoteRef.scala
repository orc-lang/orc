//
// RemoteRef.scala -- Scala traits RemoteRef and RemoteRefIdManager
// Project OrcScala
//
// Created by jthywiss on Jan 5, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import java.util.concurrent.atomic.AtomicLong

import orc.OrcRuntime
import orc.run.core.{ Blockable, Blocker }

/** A reference to an object at another Location.
  *
  * @author jthywiss
  */
trait RemoteRef extends Blocker {
  type RemoteRefId = Long
  //  val homeLocation: Location
  val remoteRefId: RemoteRefId
  //  def get(): AnyRef
  //  def deallocRemote(): Unit
  override def toString = super.toString + s"(remoteRefId=$remoteRefId)"
}

/** A mix-in to manage remote reference IDs
  *
  * @author jthywiss
  */
trait RemoteRefIdManager { self: DOrcExecution =>

  require(followerExecutionNum < 922337203)
  private val remoteRefIdCounter = new AtomicLong(10000000000L * followerExecutionNum + 1L)

  protected def freshRemoteRefId() = remoteRefIdCounter.getAndIncrement()

  protected def homeLocationForRemoteRef(id: Long): PeerLocation = {
    val followerNum = id / 10000000000L
    assert(followerNum <= Int.MaxValue && followerNum >= Int.MinValue)
    val home = locationForFollowerNum(followerNum.toInt)
    assert(home != null, s"homeLocationFor $id should not be null")
    home
  }

  def freshGroupProxyId(): GroupProxyId = freshRemoteRefId()
  def freshRemoteObjectId(): RemoteObjectRef#RemoteRefId = freshRemoteRefId()
  def freshRemoteFutureId(): RemoteFutureRef#RemoteRefId = freshRemoteRefId()

  def homeLocationForGroupProxyId(id: GroupProxyId) = homeLocationForRemoteRef(id)
  def homeLocationForRemoteObjectId(id: RemoteObjectRef#RemoteRefId) = homeLocationForRemoteRef(id)
  def homeLocationForRemoteFutureId(id: RemoteFutureRef#RemoteRefId) = homeLocationForRemoteRef(id)

}

/** A reference to an Orc value at another Location.
  *
  * @author jthywiss
  */
class RemoteObjectRef(val remoteRefId: RemoteObjectRef#RemoteRefId) extends RemoteRef {
  override type RemoteRefId = Long
  override def check(t: Blockable): Unit = ???
}

/** A mix-in to manage remote object references.
  *
  * @author jthywiss
  */
trait RemoteObjectManager { self: DOrcExecution =>

  // These two maps are inverses of each other
  protected val remotedObjects = new java.util.concurrent.ConcurrentHashMap[AnyRef, RemoteObjectRef#RemoteRefId]
  protected val remotedObjectIds = new java.util.concurrent.ConcurrentHashMap[RemoteObjectRef#RemoteRefId, AnyRef]
  protected val remotedObjectUpdateLock = new Object()

  def remoteIdForObject(obj: AnyRef): RemoteObjectRef#RemoteRefId = {
    //Logger.entering(getClass.getName, "idForObject")
    obj match {
      case ro: RemoteObjectRef => ro.remoteRefId
      case _ => remotedObjectUpdateLock synchronized {
        if (remotedObjects.contains(obj)) {
          remotedObjects.get(obj)
        } else {
          val newObjId = freshRemoteObjectId()
          remotedObjects.put(obj, newObjId)
          remotedObjectIds.put(newObjId, obj)
          newObjId
        }
      }
    }
  }

  def localObjectForRemoteId(objectId: RemoteObjectRef#RemoteRefId): Option[AnyRef] = Option(remotedObjectIds.get(objectId))

}
