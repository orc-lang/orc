//

// LocationPinnedTuple.scala -- Scala classes LocationPinnedTupleConstructor and LocationPinnedTuple
// Project OrcTests
//
// Created by jthywiss on Jun 27, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.run.distrib.{ AbstractLocation, CallLocationOverrider, ClusterLocations, ValueLocator, ValueLocatorFactory }
import orc.values.OrcTuple
import orc.values.sites.{ JavaMemberProxy, JavaStaticMemberProxy }

/** An Orc tuple which has a location policy that only permits a given
  * location.
  *
  * @author jthywiss
  */
class LocationPinnedTuple(val locationNum: Int, args: Array[AnyRef]) extends OrcTuple(args) 

/** A ValueLocator that maps LocationPinnedTuple instances to a location based
  * on the vertex name.
  *
  * @author jthywiss
  */
class LocationPinnedTupleValueLocator[L <: AbstractLocation](locations: ClusterLocations[L]) extends ValueLocator[L] with CallLocationOverrider[L] {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = locations.allLocations.size

  /* sort by hashCode to give a stable order */
  private val locIndex = locations.allLocations.toSeq.sortWith({(x, y) => x.hashCode.compareTo(y.hashCode) < 0 })
  @inline
  private def locationFromNum(locationNum: Int) = locIndex((locationNum % numLocations - 1) + 1)

  override val currentLocations: PartialFunction[Any, Set[L]] = {
    case lpt: LocationPinnedTuple => Set(locationFromNum(lpt.locationNum))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[LocationPinnedTuple] && jmp.theObject != null => Set(locationFromNum(jmp.theObject.asInstanceOf[LocationPinnedTuple].locationNum))
  }

  override val valueIsLocal: PartialFunction[Any, Boolean] = {
    case lpt: LocationPinnedTuple => locationFromNum(lpt.locationNum) == locations.here
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[LocationPinnedTuple] && jmp.theObject != null => locationFromNum(jmp.theObject.asInstanceOf[LocationPinnedTuple].locationNum) == locations.here
  }

  override val permittedLocations: PartialFunction[Any, Set[L]] = {
    case lpt: LocationPinnedTuple => Set(locationFromNum(lpt.locationNum))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[LocationPinnedTuple] && jmp.theObject != null => Set(locationFromNum(jmp.theObject.asInstanceOf[LocationPinnedTuple].locationNum))
  }

  override def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[LocationPinnedTuple] && jsmp.memberName == "apply" => {
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"LocationPinnedTuple.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val mustMigrate = locationFromNum(arguments(0).asInstanceOf[Number].intValue) != locations.here
        orc.run.distrib.Logger.Invoke.finer("LocationPinnedTupleValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy returning " + mustMigrate)
        Some(mustMigrate)
      }
      case _ => {
        None
      }
    }
  }

  override def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[L] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[LocationPinnedTuple] && jsmp.memberName == "apply" => {
        orc.run.distrib.Logger.Invoke.finer("LocationPinnedTupleValueLocator.callLocationOverride JavaStaticMemberProxy case")
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"LocationPinnedTuple.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val loc = locationFromNum(arguments(0).asInstanceOf[Number].intValue)
        orc.run.distrib.Logger.Invoke.finer("LocationPinnedTupleValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy callLocationOverride location:" + loc)
        Set(loc)
      }
    }
  }

}

/** Service provider to get LocationPinnedTupleValueLocator instance.
  *
  * @author jthywiss
  */
class LocationPinnedTupleValueLocatorFactory[L <: AbstractLocation] extends ValueLocatorFactory[L] {
  override def apply(locations: ClusterLocations[L]): LocationPinnedTupleValueLocator[L] = new LocationPinnedTupleValueLocator[L](locations)
}
