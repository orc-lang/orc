//
// BoundedChannel.java -- Java class BoundedChannel
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.util.ArrayList

import java.util.LinkedList

import orc.MaterializedCallContext

import orc.error.runtime.ArityMismatchException

import orc.error.runtime.TokenException

import orc.lib.state.types.BoundedChannelType

import orc.run.distrib.AbstractLocation

import orc.run.distrib.ClusterLocations

import orc.run.distrib.DOrcPlacementPolicy

import orc.types.Type

import orc.values.sites.{ TypedSite, Site0Simple, Site1Simple, TotalSite0Simple }

import orc.values.sites.TotalSite1Simple
import orc.values.{Signal, FastObject, Field, NumericsConfig}
import orc.{VirtualCallContext, SiteResponseSet}
import orc.values.sites.SiteMetadata
import orc.values.sites.NonBlockingSite

/**
  * A bounded channel. With a bound of zero, behaves as a synchronous channel.
  *
  * @author quark
  */
class BoundedChannel extends TotalSite1Simple[Number] with TypedSite {
  override def eval(bound: Number): AnyRef = {
      new BoundedChannel.Instance(bound.intValue)
  }

  override def orcType(): Type = BoundedChannelType.getBuilder
}

object BoundedChannel {
  private val members =
    FastObject.members("get", "getD", "put", "putD", "getAll", "getOpen", "getBound", "close", "closeD")

  class Instance(private var open: Int)
      extends FastObject(members) with DOrcPlacementPolicy {
    val contents = new LinkedList[AnyRef]()
    val readers = new LinkedList[MaterializedCallContext]()
    val writers = new LinkedList[MaterializedCallContext]()

    private[this] var closer: MaterializedCallContext = null
    private[this] var closed: Boolean = false

    private def publishFromContents(ctx: VirtualCallContext, v: AnyRef) = {
      val r1 = ctx.publish(v)
      val r2 = if (writers.isEmpty) {
        open += 1
        r1
      } else {
        val writer = writers.removeFirst()
        r1.publish(writer, Signal)
      }
      val r3 = if (closer != null && contents.isEmpty) {
        val mctx = closer
        closer = null
        r2.publish(mctx, Signal)
      } else r2
      r3
    }

    val getSite = new Site0Simple {
      def eval(ctx: VirtualCallContext) = Instance.this synchronized {
        contents.poll() match {
          case null =>
            if (closed) {
              ctx.halt()
            } else {
              val mctx = ctx.materialize()
              mctx.setQuiescent()
              readers.add(mctx)
              ctx.empty
            }
          case v =>
            publishFromContents(ctx, v)
        }
      }
    }

    val getDSite = new Site0Simple with NonBlockingSite {
      def eval(ctx: VirtualCallContext) = Instance.this synchronized {
        contents.poll() match {
          case null =>
            ctx.halt()
          case v =>
            publishFromContents(ctx, v)
        }
      }
    }

    abstract class PutSiteBase extends Site1Simple[AnyRef] {
      def eval(writer: VirtualCallContext, item: AnyRef) = Instance.this synchronized {
        if (closed) {
          writer.halt()
        } else if (!readers.isEmpty) {
          val reader = readers.removeFirst()
          writer.publish(Signal).publish(reader, item)
        } else if (open == 0) {
          handleNoSlot(writer, item)
        } else {
          contents.addLast(item)
          open -= 1
          writer.publish(Signal)
        }
      }
      def handleNoSlot(writer: VirtualCallContext, item: AnyRef): SiteResponseSet
    }

    val putSite = new PutSiteBase {
      def handleNoSlot(writer: VirtualCallContext, item: AnyRef) = {
        contents.addLast(item)
        val mctx = writer.materialize()
        mctx.setQuiescent()
        writers.addLast(mctx)
        writer.empty
      }
    }

    val putDSite = new PutSiteBase with NonBlockingSite {
      def handleNoSlot(writer: VirtualCallContext, item: AnyRef) = {
        writer.halt()
      }
    }

    import scala.collection.JavaConversions._

    val getAllSite = new Site0Simple {
      def eval(ctx: VirtualCallContext) = Instance.this synchronized {
        // restore open slots
        open += contents.size - writers.size
        // collect all values in a list
        val out: AnyRef = contents.toList
        contents.clear()
        val r0 = ctx.publish(out)
        val oldWriters = new ArrayList[MaterializedCallContext](writers)
        writers.clear()
        // resume all writers
        val r1 = oldWriters.foldLeft(r0)(_.publish(_, Signal))
        // notify closer if necessary
        val r2 = if (closer != null) {
          val mctx = closer
          closer = null
          r1.publish(mctx, Signal)
        } else {
          r1
        }
        r2
      }
    }

    val getOpenSite = new TotalSite0Simple {
      def eval() = NumericsConfig.toOrcIntegral(open)
    }

    val getBoundSite = new TotalSite0Simple {
      def eval() = Instance.this synchronized {
        NumericsConfig.toOrcIntegral(open + contents.size - writers.size)
      }
    }

    val isClosedSite = new TotalSite0Simple {
      override def eval() = closed
    }

    val closeSite = new Site0Simple with SiteMetadata {
      def eval(ctx: VirtualCallContext) = Instance.this synchronized {
        closed = true
        val r0 = if (contents.isEmpty) {
          ctx.publish(Signal)
        } else {
          closer = ctx.materialize()
          closer.setQuiescent()
          ctx.empty
        }
        // resume all writers
        val r1 = readers.foldLeft(r0)(_.publish(_, Signal))
        r1
      }
    }

    val closeDSite = new Site0Simple with NonBlockingSite {
      def eval(ctx: VirtualCallContext) = Instance.this synchronized {
        closed = true
        val r0 = ctx.publish(Signal)
        // resume all writers
        val r1 = readers.foldLeft(r0)(_.publish(_, Signal))
        r1
      }
    }

    protected val values: Array[AnyRef] = Array(getSite, getDSite, putSite, putDSite, getAllSite, getOpenSite, getBoundSite, closeSite, closeDSite)


    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet

  }

}
