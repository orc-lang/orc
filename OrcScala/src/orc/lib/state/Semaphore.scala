//
// Semaphore.java -- Java class Semaphore
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;
import scala.collection.mutable.Queue

import orc.{MaterializedCallContext, VirtualCallContext}
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.TokenException
import orc.lib.state.types.SemaphoreType
import orc.run.distrib.AbstractLocation
import orc.run.distrib.ClusterLocations
import orc.run.distrib.DOrcPlacementPolicy
import orc.types.Type
import orc.values.sites.{TypedSite, FunctionalSite}
import orc.values.sites.{ TotalSite1Simple, Site0Simple }
import orc.values.FastObject
import orc.values.Signal

/**
 * @author quark
 * @author dkitchin
 */
class Semaphore extends TotalSite1Simple[Number] with TypedSite with FunctionalSite {

  def eval(arg: Number) = {
    val initialValue = arg.intValue
    if (initialValue >= 0) {
        new Semaphore.Instance(initialValue);
    } else {
        throw new IllegalArgumentException("Semaphore requires a non-negative argument");
    }
  }

  def orcType() = {
      SemaphoreType.getBuilder
  }
}

object Semaphore {
  private val members = FastObject.members("acquire", "acquireD", "release", "snoop", "snoopD")

  class Instance(protected var n: Int) extends FastObject(members) with DOrcPlacementPolicy {
    instance =>

      require(n >= 0)

      val waiters = Queue[MaterializedCallContext]()
      var snoopers = Queue[MaterializedCallContext]()

      protected val values = Array(
          // "acquire" ->
          new Site0Simple() {
            def eval(ctx: VirtualCallContext) = instance synchronized {
                        if (0 == n) {
                          val mat = ctx.materialize()
                          mat.setQuiescent()
                          waiters += mat
                          if (snoopers.nonEmpty) {
                            val oldSnoopers = snoopers.clone()
                            snoopers.clear()
                            oldSnoopers.foreach(_.publish(Signal))
                          }
                          ctx.empty
                        } else {
                          n -= 1
                          ctx.publish(Signal)
                        }
                }
            },
            // "acquireD" ->
            new Site0Simple() {
              def eval(ctx: VirtualCallContext) = instance synchronized {
                        if (0 == n) {
                            ctx.halt();
                        } else {
                          n -= 1
                          ctx.publish(Signal);
                        }
                }
              val nonBlocking = true
            },
            // "release" ->
            new Site0Simple() {
              def eval(ctx: VirtualCallContext) = instance synchronized {
                        val r = if (waiters.isEmpty) {
                          n += 1
                            ctx.empty
                        } else {
                            val waiter = waiters.dequeue()
                            ctx.empty.publish(waiter, Signal)
                        }
                        r.publish(ctx, Signal)
                    }
              val nonBlocking = true
            },
            // "snoop" ->
            new Site0Simple() {
              def eval(ctx: VirtualCallContext) = instance synchronized {
                        if (waiters.isEmpty) {
                          val mat = ctx.materialize()
                          mat.setQuiescent()
                          snoopers += mat
                          ctx.empty
                        } else {
                            ctx.publish(Signal)
                        }
                }
            },
            // "snoopD" ->
            new Site0Simple() {
              def eval(ctx: VirtualCallContext) = instance synchronized {
                        if (waiters.isEmpty) {
                            ctx.halt();
                        } else {
                            ctx.publish(Signal);
                        }
                }
              val nonBlocking = true
            }
            )

      def permittedLocations[L <: AbstractLocation](locations: ClusterLocations[L]): collection.immutable.Set[L] = {
          locations.hereSet
      }
    }
}
