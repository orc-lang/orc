//
// VertexValueLocator.scala -- Scala class VertexValueLocator
// Project OrcTests
//
// Created by jthywiss on Mar 20, 2018.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.run.distrib.{ AbstractLocation, CallLocationOverrider, ClusterLocations, ValueLocator, ValueLocatorFactory }
import orc.values.sites.{ JavaMemberProxy, JavaStaticMemberProxy }

/** A ValueLocator that maps VertexWithPathLen instances to a location based
  * on the vertex name.
  *
  * @author jthywiss
  */
class VertexValueLocator[L <: AbstractLocation](locations: ClusterLocations[L]) extends ValueLocator[L] with CallLocationOverrider[L] {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = locations.allLocations.size

  /* sort by hashCode to give a stable order */
  private val locIndex = locations.allLocations.toSeq.sortWith({(x, y) => x.hashCode.compareTo(y.hashCode) < 0 })
  @inline
  private def locationFromVertexName(name: Int) = locIndex((name % numLocations - 1) + 1)

  override val currentLocations: PartialFunction[Any, Set[L]] = {
    case v: VertexWithPathLen => Set(locationFromVertexName(v.name))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => Set(locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name))
  }

  override val valueIsLocal: PartialFunction[Any, Boolean] = {
    case v: VertexWithPathLen => locationFromVertexName(v.name) == locations.here
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name) == locations.here
  }

  override val permittedLocations: PartialFunction[Any, Set[L]] = {
    case v: VertexWithPathLen => Set(locationFromVertexName(v.name))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => Set(locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name))
  }

  override def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[VertexWithPathLen] && jsmp.memberName == "apply" => {
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"VertexWithPathLen.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val mustMigrate = locationFromVertexName(arguments(0).asInstanceOf[Number].intValue) != locations.here
        orc.run.distrib.Logger.Invoke.finer("VertexValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy returning " + mustMigrate)
        Some(mustMigrate)
      }
      case _: orc.test.item.distrib.GraphOfVertexWithPathLen => Some(locations.here != locations.leader)
      case jmp: JavaMemberProxy if jmp.javaClass == classOf[GraphOfVertexWithPathLen] => Some(locations.leader != locations.here)
      case _ => {
        None
      }
    }
  }

  override def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[L] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[VertexWithPathLen] && jsmp.memberName == "apply" => {
        orc.run.distrib.Logger.Invoke.finer("VertexValueLocator.callLocationOverride JavaStaticMemberProxy case")
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"VertexWithPathLen.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val loc = locationFromVertexName(arguments(0).asInstanceOf[Number].intValue)
        orc.run.distrib.Logger.Invoke.finer("VertexValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy callLocationOverride location:" + loc)
        Set(loc)
      }
      case _: orc.test.item.distrib.GraphOfVertexWithPathLen => locations.leaderSet
      case jmp: JavaMemberProxy if jmp.javaClass == classOf[GraphOfVertexWithPathLen] => locations.leaderSet
      case _ => {
        Set.empty
      }
    }
  }

}

/** Service provider to get VertexValueLocator instance.
  *
  * @author jthywiss
  */
class VertexValueLocatorFactory[L <: AbstractLocation] extends ValueLocatorFactory[L] {
  override def apply(locations: ClusterLocations[L]): VertexValueLocator[L] = new VertexValueLocator[L](locations)
}
