//
// Counter.java -- Java class Counter
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.math.BigDecimal

import java.util.LinkedList

import orc.VirtualCallContext

import orc.MaterializedCallContext

import orc.error.runtime.ArityMismatchException

import orc.error.runtime.TokenException

import orc.lib.state.types.CounterType

import orc.run.distrib.AbstractLocation

import orc.run.distrib.ClusterLocations

import orc.run.distrib.DOrcPlacementPolicy

import orc.types.Type

import orc.values.sites.TypedSite











import orc.values._
import orc.values.sites._
import scala.collection.JavaConverters._
import orc.values.sites.SiteMetadata

/**
  * Factory for counters.
  *
  * @author quark
  */
class Counter extends TotalSite1Simple[Number] with TypedSite with FunctionalSite {
  override def eval(init: Number): AnyRef = {
    new Counter.Instance(init.intValue)
  }

  override def orcType(): Type = CounterType.getBuilder
}

object Counter {
  private val members = FastObject.members("inc", "dec", "onZero", "value")

  class Instance(protected var count: Int)
      extends FastObject(members)
      with DOrcPlacementPolicy {

    protected val waiters: LinkedList[MaterializedCallContext] =
      new LinkedList[MaterializedCallContext]()

    val values = Array(
        // "inc",
        new TotalSite0Simple with NonBlockingSite {
        override def eval(): AnyRef = {
          Counter.this.synchronized { count += 1 }
          Signal
        }
      },
      // "dec",
        new Site0Simple with NonBlockingSite {
        override def eval(ctx: VirtualCallContext) =
          Counter.this.synchronized {
              if (count > 0) {
                val r1 = ctx.publish(Signal)
                if (count == 0) {
                  val oldWaiters = waiters.asScala.toArray
                  waiters.clear()
                  oldWaiters.foldLeft(r1)(_.publish(_, Signal))
                } else r1
              } else {
                ctx.empty
              }
            }
        },
        // "onZero",
        new Site0Simple with SiteMetadata {
        override def eval(caller: VirtualCallContext) =
          Counter.this.synchronized {
              if (count == 0) {
                caller.publish(Signal)
              } else {
                val mat = caller.materialize()
                mat.setQuiescent()
                waiters.add(mat)
                caller.empty
              }
            }
        },
      // "value",
        new TotalSite0Simple with NonBlockingSite {
        override def eval(): AnyRef = NumericsConfig.toOrcIntegral(count)
      },
    )

    override def permittedLocations[L <: AbstractLocation](
        locations: ClusterLocations[L]): scala.collection.immutable.Set[L] =
      locations.hereSet

  }



}

