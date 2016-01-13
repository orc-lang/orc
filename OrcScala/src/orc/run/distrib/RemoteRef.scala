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
  type RemoteRefId
  //  val homeLocation: Location
  val remoteRefId: RemoteRefId
  //  def get(): AnyRef
  //  def deallocRemote(): Unit
}

/** A mix-in to manage remote reference IDs
  *
  * @author jthywiss
  */
trait RemoteRefIdManager { self: DOrcExecution =>

  require(followerExecutionNum < 922337203)
  private val remoteRefIdCounter = new AtomicLong(10000000000L * followerExecutionNum + 1L)

  protected def freshRemoteRefId() = remoteRefIdCounter.getAndIncrement()

  protected def homeLocationForRemoteRef(id: Long): Location = {
    val followerNum = id / 10000000000L
    assert(followerNum <= Int.MaxValue && followerNum >= Int.MinValue)
    locationForFollowerNum(followerNum.toInt)
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
class RemoteObjectRef(val homeLocation: Location, val remoteRefId: RemoteObjectRef#RemoteRefId) extends RemoteRef {
  type RemoteRefId = Long
  override val runtime: OrcRuntime = ???
  override def check(t: Blockable): Unit = ???
}

object RemoteObjectRef {
  def idForObject(obj: AnyRef): RemoteObjectRef#RemoteRefId = {
    obj match {
      case rg: RemoteObjectRef => rg.remoteRefId
      case _ => ???
    }
  }
  def objectForId(objectId: RemoteObjectRef#RemoteRefId): AnyRef = ???
}
