//
// RemoteRef.scala -- Scala traits RemoteRef and RemoteRefIdManager, and classes RemoteObjectRef and RemoteObjectRefReplacement
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
trait RemoteRefIdManager {
  self: DOrcExecution =>

  private val remoteRefIdCounter = new AtomicLong(followerExecutionNum.toLong << 32)

  protected def freshRemoteRefId(): RemoteObjectRef#RemoteRefId = remoteRefIdCounter.getAndIncrement()

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

  def homeLocationForGroupProxyId(id: GroupProxyId): PeerLocation = homeLocationForRemoteRef(id)
  def homeLocationForRemoteObjectId(id: RemoteObjectRef#RemoteRefId): PeerLocation = homeLocationForRemoteRef(id)
  def homeLocationForRemoteFutureId(id: RemoteFutureRef#RemoteRefId): PeerLocation = homeLocationForRemoteRef(id)

}

/** A reference to an Orc value at another Location.
  *
  * @author jthywiss
  */
class RemoteObjectRef(override val remoteRefId: RemoteObjectRef#RemoteRefId) extends RemoteRef {
  override type RemoteRefId = Long

  override def toString: String = super.toString + f"(remoteRefId=$remoteRefId%#x)"

  def marshal(): RemoteObjectRefReplacement = {
    RemoteObjectRefReplacement(remoteRefId)
  }
}

/** A serialization replacement for a RemoteObjectRef.
  *
  * @author jthywiss
  */
case class RemoteObjectRefReplacement(remoteRefId: RemoteObjectRef#RemoteRefId) {
  override def toString: String = this.productPrefix + "(0x" + remoteRefId.toHexString + ")"
  def unmarshal(rmtObjMgr: RemoteObjectManager): AnyRef = {
    rmtObjMgr.localObjectForRemoteId(remoteRefId).getOrElse(new RemoteObjectRef(remoteRefId))
  }
}
