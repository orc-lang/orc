//
// LocationPinnedTuple.scala -- Scala classes LocationPinnedTupleConstructor and LocationPinnedTuple
// Project PorcE
//
// Created by jthywiss on Jun 27, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.VirtualCallContext
import orc.error.OrcException
import orc.values.OrcTuple
import orc.values.sites.Site
import orc.run.distrib.DOrcPlacementPolicy
import orc.run.distrib.ClusterLocations
import orc.run.distrib.AbstractLocation
import orc.OrcRuntime
import orc.Invoker
import orc.SiteResponseSet

/** Superclass of Orc sites to construct LocationPinnedTuples
  *
  * @author jthywiss
  */
sealed abstract class LocationPinnedTupleConstructor(locationNum: Int) extends Site with DOrcPlacementPolicy with Serializable {
  self =>

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    new Invoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        target == self
      }
      def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
        try {
          orc.run.StopWatches.implementation {
            Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "invoke", args)
            val res = try {
              val loc = ctx.execution.asInstanceOf[DOrcExecution].locationForFollowerNum(locationNum)
              new LocationPinnedTuple(loc, args)
            } catch {
              case _: ClassCastException =>
                new OrcTuple(args)
            }
            ctx.publish(res)
          }
        } catch {
          case e: OrcException =>
            ctx.halt(e)
        }
      }
    }
  }

  override def permittedLocations[L <: AbstractLocation](locations: ClusterLocations[L]) = {
    locations match {
      case runtime: DOrcRuntime => Set(runtime.locationForRuntimeId(locationNum))
    }
  }
}

object Location0PinnedTupleConstructor extends LocationPinnedTupleConstructor(0) {}
object Location1PinnedTupleConstructor extends LocationPinnedTupleConstructor(1) {}
object Location2PinnedTupleConstructor extends LocationPinnedTupleConstructor(2) {}
object Location3PinnedTupleConstructor extends LocationPinnedTupleConstructor(3) {}
object Location4PinnedTupleConstructor extends LocationPinnedTupleConstructor(4) {}
object Location5PinnedTupleConstructor extends LocationPinnedTupleConstructor(5) {}
object Location6PinnedTupleConstructor extends LocationPinnedTupleConstructor(6) {}
object Location7PinnedTupleConstructor extends LocationPinnedTupleConstructor(7) {}
object Location8PinnedTupleConstructor extends LocationPinnedTupleConstructor(8) {}
object Location9PinnedTupleConstructor extends LocationPinnedTupleConstructor(9) {}
object Location10PinnedTupleConstructor extends LocationPinnedTupleConstructor(10) {}
object Location11PinnedTupleConstructor extends LocationPinnedTupleConstructor(11) {}
object Location12PinnedTupleConstructor extends LocationPinnedTupleConstructor(12) {}

/** An Orc tuple which has a location policy that only permits a given
  * location.
  *
  * @author jthywiss
  */
class LocationPinnedTuple(l: PeerLocation, args: Array[AnyRef]) extends OrcTuple(args) with DOrcPlacementPolicy {
  private val permittedLocationSet = Set(l)
  override def permittedLocations[L <: AbstractLocation](locations: ClusterLocations[L]) = {
    locations match {
      case runtime: DOrcRuntime => permittedLocationSet
    }
  }
}
