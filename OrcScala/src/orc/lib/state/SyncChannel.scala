//
// SyncChannel.java -- Java class SyncChannel
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.util.LinkedList

import orc.CallContext

import orc.MaterializedCallContext

import orc.error.runtime.TokenException

import orc.lib.state.types.SyncChannelType

import orc.run.distrib.AbstractLocation

import orc.run.distrib.ClusterLocations

import orc.run.distrib.DOrcPlacementPolicy

import orc.types.Type

import orc.values.sites.TypedSite









import orc.values._
import orc._
import orc.values.sites._
import scala.collection.JavaConversions._
import orc.values.sites.TotalSite1Simple

/**
  * Implements the local site SyncChannel, which creates synchronous channels.
  *
  * @author dkitchin
  */
object SyncChannel extends TotalSite0Simple with TypedSite {

  override def eval(): AnyRef = new SyncChannel.Instance()

  override def orcType(): Type = SyncChannelType.getBuilder

  protected case class SenderItem(sender: MaterializedCallContext,
                           sent: AnyRef)

  val members = FastObject.members("get", "put")

  protected class Instance()
      extends FastObject(members)
      with DOrcPlacementPolicy {

// Invariant: senderQueue is empty or receiverQueue is empty
    protected val senderQueue: LinkedList[SenderItem] = new LinkedList()

    protected val receiverQueue: LinkedList[MaterializedCallContext] =
      new LinkedList[MaterializedCallContext]()

    val values = Array(new getMethod(), new putMethod())

    protected class getMethod extends Site0Simple {
      override def eval(receiver: VirtualCallContext) = {
        if (senderQueue.isEmpty) {
// If there are no waiting senders, put this caller on the queue
          val mat = receiver.materialize()
          mat.setQuiescent()
          receiverQueue.addLast(mat)
          receiver.empty
        } else
          {
          // If there is a waiting sender, both sender and receiver return
            val SenderItem(sender, item) = senderQueue.removeFirst()
            receiver.publish(item).publish(sender, Signal)
          }
      }

    }

    protected class putMethod extends Site1Simple[AnyRef] {

      override def eval(sender: VirtualCallContext, item: AnyRef) = {
        if (receiverQueue.isEmpty) {
// If there are no waiting receivers, put this sender on the
// queue
          val mat = sender.materialize()
          mat.setQuiescent()
          senderQueue.addLast(SenderItem(mat, item))
          sender.empty
        } else
          {
// If there is a waiting receiver, both receiver and sender, return.
            val receiver = receiverQueue.removeFirst()

            sender.publish(Signal).publish(receiver, item)
          }
      }

    }

    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet

  }

}

