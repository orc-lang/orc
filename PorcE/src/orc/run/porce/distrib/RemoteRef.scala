//
// RemoteRef.scala -- Scala traits RemoteRef and RemoteRefIdManager
// Project PorcE
//
// Created by jthywiss on Jan 5, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import java.util.concurrent.atomic.AtomicLong

/** A reference to a value or future at another Location.
  *
  * @author jthywiss
  */
trait RemoteRef {
  type RemoteRefId = Long
  val remoteRefId: RemoteRefId
  // Possible enhancements:
  //  val homeLocation: Location
  //  def get(): AnyRef
  //  def deallocRemote(): Unit
}

/** A mix-in to manage remote reference IDs
  *
  * @author jthywiss
  */
trait RemoteRefIdManager { self: DOrcExecution =>

  private val remoteRefIdCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  protected def freshRemoteRefId() = remoteRefIdCounter.getAndIncrement()

  protected def homeLocationForRemoteRef(id: RemoteRef#RemoteRefId): PeerLocation = {
    val followerNum = id.asInstanceOf[Long] >> 32
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
class RemoteObjectRef(override val remoteRefId: RemoteObjectRef#RemoteRefId) extends RemoteRef {
  override type RemoteRefId = Long

  override def toString = super.toString + f"(remoteRefId=$remoteRefId%#x)"

  def marshal(): RemoteObjectRefReplacement = {
    RemoteObjectRefReplacement(remoteRefId)
  }
}

/** A serialization replacement for a RemoteObjectRef.
  *
  * @author jthywiss
  */
case class RemoteObjectRefReplacement(remoteRefId: RemoteObjectRef#RemoteRefId) {
  override def toString = this.productPrefix + "(0x" + remoteRefId.toHexString + ")"
  def unmarshal(rmtObjMgr: RemoteObjectManager) = {
    rmtObjMgr.localObjectForRemoteId(remoteRefId).getOrElse(new RemoteObjectRef(remoteRefId))
  }
}

///** A mix-in to manage remote object references.
//  *
//  * @author jthywiss
//  */
//trait RemoteObjectManager { self: DOrcExecution =>
//
//  // These two maps are inverses of each other
//  protected val remotedObjects = new java.util.concurrent.ConcurrentHashMap[AnyRef, RemoteObjectRef#RemoteRefId]
//  protected val remotedObjectIds = new java.util.concurrent.ConcurrentHashMap[RemoteObjectRef#RemoteRefId, AnyRef]
//  protected val remotedObjectUpdateLock = new Object()
//
//  def remoteIdForObject(obj: AnyRef): RemoteObjectRef#RemoteRefId = {
//    //Logger.entering(getClass.getName, "idForObject", Seq(obj))
//    obj match {
//      case ro: RemoteObjectRef => ro.remoteRefId
//      case _ => remotedObjectUpdateLock synchronized {
//        if (remotedObjects.contains(obj)) {
//          remotedObjects.get(obj)
//        } else {
//          val newObjId = freshRemoteObjectId()
//          remotedObjects.put(obj, newObjId)
//          remotedObjectIds.put(newObjId, obj)
//          newObjId
//        }
//      }
//    }
//  }
//
//  def localObjectForRemoteId(objectId: RemoteObjectRef#RemoteRefId): Option[AnyRef] = Option(remotedObjectIds.get(objectId))
//
//}
