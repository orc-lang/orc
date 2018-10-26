//
// RemoteRef.scala -- Scala traits RemoteRef and RemoteRefIdManager, and classes RemoteObjectRef and RemoteObjectRefReplacement
// Project PorcE
//
// Created by jthywiss on Jan 5, 2016.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import java.util.concurrent.atomic.AtomicLong

/** A reference to a value or future at another Location.
  *
  * @author jthywiss
  */
trait RemoteRef {
  type RemoteRefId = Long
  val remoteRefId: RemoteRefId
  def canBeUsedLocally: Boolean
  // Possible enhancements:
  //  val homeLocation: Location
  //  def get(): AnyRef
  //  def deallocRemote(): Unit
}

/** A mix-in to manage remote reference IDs
  *
  * @author jthywiss
  */
trait RemoteRefIdManager {
  execution: DOrcExecution =>

  private val remoteRefIdCounter = new AtomicLong(execution.followerExecutionNum.toLong << 32)

  protected def freshRemoteRefId(): RemoteRef#RemoteRefId = remoteRefIdCounter.getAndIncrement()

  protected def homeLocationForRemoteRef(id: RemoteRef#RemoteRefId): PeerLocation = {
    val followerNum = id.asInstanceOf[Long] >> 32
    assert(followerNum <= Int.MaxValue && followerNum >= Int.MinValue)
    val home = execution.locationForFollowerNum(followerNum.toInt)
    assert(home != null, s"homeLocationFor $id should not be null")
    home
  }

}

/** A reference to an Orc value at another Location.
  *
  * @author jthywiss
  */
class RemoteObjectRef(override val remoteRefId: RemoteObjectRef#RemoteRefId) extends RemoteRef {

  override def canBeUsedLocally: Boolean = false

  override def toString: String = f"${getClass.getName}(remoteRefId=$remoteRefId%#x)"

  def marshal(): RemoteObjectRefReplacement = {
    RemoteObjectRefReplacement(remoteRefId)
  }
}

/** A serialization replacement for a RemoteObjectRef.
  *
  * @author jthywiss
  */
case class RemoteObjectRefReplacement(remoteRefId: RemoteObjectRef#RemoteRefId) extends Serializable {

  override def toString: String = f"$productPrefix(remoteRefId=$remoteRefId%#x)"

  def unmarshal(rmtObjMgr: RemoteObjectManager): AnyRef = {
    rmtObjMgr.localObjectForRemoteId(remoteRefId).getOrElse(new RemoteObjectRef(remoteRefId))
  }
}

/** A utility class to store a bidirectional mapping between object and Id.
  *
  * It provides thread-safe get-or-update operations.
  */
class ObjectRemoteRefIdMapping[T >: Null](freshRemoteRefId: () => RemoteRef#RemoteRefId) {
  // These two maps are inverses of each other
  protected val objectToRemoteId = new java.util.concurrent.ConcurrentHashMap[T, RemoteRef#RemoteRefId]()
  protected val remoteIdToObject = new java.util.concurrent.ConcurrentHashMap[RemoteRef#RemoteRefId, T]()

  def idForObject(obj: T): RemoteRef#RemoteRefId = {
    //Logger.Marshal.entering(getClass.getName, "idForObject", Seq(obj))
    obj match {
      case ro: RemoteObjectRef =>
        ro.remoteRefId
      case _ =>
        val id = objectToRemoteId.getOrDefault(obj, 0L)
        if (id == 0L) {
          // This is safe since if the value has been set between the get above and this code we will get the existing value here.
          // This is just an optimization to avoid consuming IDs and constantly writing to remoteIdToObject.
          val newObjId = objectToRemoteId.computeIfAbsent(obj, _ => freshRemoteRefId())
          assert(newObjId != 0L)
          Logger.Marshal.fine(f"Assigned $newObjId%#x to $obj: ${orc.util.GetScalaTypeName(obj)}")
          remoteIdToObject.put(newObjId, obj)
          newObjId
        } else {
          id
        }
    }
  }

  def objectForId(objectId: RemoteRef#RemoteRefId): Option[T] = Option(remoteIdToObject.get(objectId))
}

/** The manager for remote object references that is mixed into the DOrcExecution.
  */
trait RemoteObjectManager {
  idmgr: RemoteRefIdManager =>

  type RemoteRefId = RemoteRef#RemoteRefId

  protected val objectMapping = new ObjectRemoteRefIdMapping[AnyRef](() => idmgr.freshRemoteRefId())

  /** Get the Id for an opaque object.
    */
  def remoteIdForObject(obj: AnyRef): RemoteRefId = objectMapping.idForObject(obj)
  /** Get an opaque object for a previously created Id.
    */
  def localObjectForRemoteId(objectId: RemoteRefId): Option[AnyRef] = objectMapping.objectForId(objectId)

}
