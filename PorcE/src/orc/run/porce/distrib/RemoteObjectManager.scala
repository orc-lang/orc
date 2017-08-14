//
// RemoteObjectManager.scala -- Traits for object and id handling in PorcE/DOrc.
// Project PorcE
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import java.util.concurrent.atomic.AtomicLong

import orc.run.porce.runtime.{ Counter, PorcEExecution, Terminator }

/** A utility class to store a bidirectional mapping between object and Id.
  *
  * It provides thread-safe get-or-update operations.
  */
final class ObjectIdMapping[T >: Null](idManager: RemoteIdManager) {
  // These two maps are inverses of each other
  protected val objectToId = new java.util.concurrent.ConcurrentHashMap[T, java.lang.Long]()
  protected val idToObject = new java.util.concurrent.ConcurrentHashMap[RemoteRef#RemoteRefId, T]()

  def idForObject(obj: T): RemoteRef#RemoteRefId = {
    //Logger.entering(getClass.getName, "idForObject", Seq(obj))
    obj match {
      case ro: RemoteObjectRef =>
        ro.remoteRefId
      case _ =>
        val id = objectToId.get(obj)
        if (id == null) {
          // This is safe since if the value has been set between the get above and this code we will get the existing value here.
          // This is just an optimization to avoid consuming Ids and constantly writing to idToObject.
          val newObjId = objectToId.computeIfAbsent(obj, _ => idManager.freshRemoteRefId())
          idToObject.put(newObjId, obj)
          newObjId
        } else {
          id
        }
    }
  }

  def objectForId(objectId: RemoteRef#RemoteRefId): Option[T] = Option(idToObject.get(objectId))
}

/** The manager for remote Ids that is mixed into the PorcEExecution.
  */
trait RemoteIdManager {
  this: PorcEExecution =>

  // FIXME: DORC: Implement these somewhere and then make them accessible here.
  def followerExecutionNum: Int
  def locationForFollowerNum(followerNum: Int): PeerLocation

  private val remoteRefIdCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  def freshRemoteRefId() = remoteRefIdCounter.getAndIncrement()

  def homeLocationForRemoteRef(id: RemoteRef#RemoteRefId): PeerLocation = {
    val followerNum = id.asInstanceOf[Long] >> 32
    assert(followerNum <= Int.MaxValue && followerNum >= Int.MinValue)
    val home = locationForFollowerNum(followerNum.toInt)
    assert(home != null, s"homeLocationFor $id should not be null")
    home
  }
}

/** The manager for remote object references that is mixed into the PorcEExecution.
  */
trait RemoteObjectManager extends RemoteIdManager {
  this: PorcEExecution =>

  type RemoteRefId = RemoteRef#RemoteRefId

  protected val objectMapping = new ObjectIdMapping[AnyRef](this)
  protected val terminatorMapping = new ObjectIdMapping[Terminator](this)
  protected val counterMapping = new ObjectIdMapping[Counter](this)
  // FIXME: DORC: It may actually be easier to just put terminators and counters into the objectMapping. If that is the case them remove the additional mappings.

  /** Get the Id for an opaque object.
    */
  def remoteIdForObject(obj: AnyRef): RemoteRefId = objectMapping.idForObject(obj)
  /** Get an opaque object for a previously created Id.
    */
  def localObjectForRemoteId(objectId: RemoteRefId): Option[AnyRef] = objectMapping.objectForId(objectId)

  /** Get the Id for a Terminator.
    */
  def remoteIdForTerminator(obj: Terminator): RemoteRefId = terminatorMapping.idForObject(obj)
  /** Get a Terminator for a previously created Id.
    */
  def terminatorForRemoteId(objectId: RemoteRefId): Option[Terminator] = terminatorMapping.objectForId(objectId)

  /** Get the Id for a Counter.
    */
  def remoteIdForCounter(obj: Counter): RemoteRefId = counterMapping.idForObject(obj)
  /** Get a Counter for a previously created Id.
    */
  def counterForRemoteId(objectId: RemoteRefId): Option[Counter] = counterMapping.objectForId(objectId)
}
