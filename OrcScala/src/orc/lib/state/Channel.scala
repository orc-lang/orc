//
// Channel.java -- Java class Channel
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.util.LinkedList

import scala.collection.JavaConverters.asScalaBufferConverter

import orc.{MaterializedCallContext, SiteResponseSet, VirtualCallContext}
import orc.lib.state.types.ChannelType
import orc.run.distrib.{AbstractLocation, ClusterLocations, DOrcPlacementPolicy}
import orc.values.{FastObject, Signal}
import orc.values.sites.{
  FunctionalSite,
  NonBlockingSite,
  Site0Simple,
  Site1Simple,
  TotalSite0Simple,
  TypedSite
}

/**
  * Implements the local site Channel, which creates asynchronous channels.
  *
  * @author cawellington, dkitchin
  */
object Channel extends TotalSite0Simple with TypedSite with FunctionalSite {
  override val inlinable = true
  override def eval(): AnyRef = new Channel.Instance()

  def orcType() = ChannelType.getBuilder

  class PutSite(val channel: Instance)
      extends Site1Simple[AnyRef]
      with NonBlockingSite {
    override val inlinable = true

    def eval(ctx: VirtualCallContext, item: AnyRef): SiteResponseSet =
      channel.synchronized {
        if (channel.closed) {
          return ctx.empty
        }
        // Since this is an asynchronous channel, a put call
        // always returns.
        val r1 = ctx.publish(Signal)
        while (true) if (channel.readers.isEmpty) {
          // If there are no waiting callers, queue this item.
          channel.contents.addLast(item)
          return r1
        } else {
          // If there are callers waiting, give this item to
          // the top caller.
          val receiver = channel.readers.removeFirst()
          if (receiver.isLive) {
            // If the reader is live then publish into it.
            return r1.publish(receiver, item)
          } else {}
          // If the reader is dead then go through the loop again to get another reader.
        }
        throw new AssertionError("Unreachable")
      }

  }

  class GetSite(val channel: Instance) extends Site0Simple {
    override val inlinable = true

    def eval(reader: VirtualCallContext) = channel.synchronized {
      if (channel.contents.isEmpty) {
        if (channel.closed) {
          reader.halt()
        } else {
          val mat = reader.materialize()
          mat.setQuiescent()
          channel.readers.addLast(mat)
          reader.empty
        }
      } else {
        // If there is an item available, pop it and return
        // it.
        val v = channel.contents.removeFirst()
        val r1 = if (channel.closer != null && channel.contents.isEmpty) {
          val closer = channel.closer
          channel.closer = null
          reader.empty.publish(closer, Signal)
        } else reader.empty
        r1.publish(reader, v)
      }
    }

  }

  private val members = FastObject.members("get",
                                           "put",
                                           "getD",
                                           "getAll",
                                           "isClosed",
                                           "close",
                                           "closeD")

  class Instance() extends FastObject(members) with DOrcPlacementPolicy {

    val contents: LinkedList[AnyRef] = new LinkedList()

    val readers: LinkedList[MaterializedCallContext] =
      new LinkedList[MaterializedCallContext]()

    var closer: MaterializedCallContext = null

    /**
      * Once this becomes true, no new items may be put, and gets on an empty
      * channel die rather than blocking.
      */
    var closed: Boolean = false

    val values = Array(
      new GetSite(Instance.this),
      new PutSite(Instance.this),
      // "getD",
      new Site0Simple with NonBlockingSite {
        override val inlinable = true
        def eval(reader: VirtualCallContext) = Instance.this.synchronized {
          if (contents.isEmpty) {
            reader.halt()
          } else {
            val r0 = reader.empty
            val r1 = if (closer != null && contents.isEmpty) {
              closer = null
              r0.publish(closer, Signal)
            } else {
              r0
            }
            r1.publish(reader, contents.removeFirst())
          }
        }
      },
      // "getAll",
      new Site0Simple {
        override val inlinable = true
        def eval(ctx: VirtualCallContext) = Instance.this.synchronized {
          val out: AnyRef = contents.asScala.toList
          contents.clear()
          val r1 = if (closer != null) {
            closer = null
            ctx.empty.publish(closer, Signal)
          } else ctx.empty
          r1.publish(ctx, out)
        }
      },
      // "isClosed",
      new TotalSite0Simple with NonBlockingSite {
        override val inlinable = true
        def eval() = Instance.this.synchronized { closed }
      },
      // "close",
      new Site0Simple {
        def eval(ctx: VirtualCallContext) = Instance.this.synchronized {
          closed = true
          val r1 = readers.asScala.foldLeft(ctx.empty)(_.halt(_))
          if (contents.isEmpty) {
            r1.publish(ctx, Signal)
          } else {
            closer = ctx.materialize()
            closer.setQuiescent()
            r1
          }
        }
      },
      // "closeD",
      new Site0Simple with NonBlockingSite {
        def eval(ctx: VirtualCallContext) = Instance.this.synchronized {
          closed = true
          val r1 = readers.asScala.foldLeft(ctx.empty)(_.halt(_))
          r1.publish(ctx, Signal)
        }
      },
    )

    override def toString(): String = Instance.this.synchronized {
      super.toString + contents.toString
    }

    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet

  }
}
