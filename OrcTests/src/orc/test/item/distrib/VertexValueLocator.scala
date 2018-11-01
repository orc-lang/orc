//
// VertexValueLocator.scala -- Scala class VertexValueLocator
// Project OrcTests
//
// Created by jthywiss on Mar 20, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.run.distrib.porce.{ CallLocationOverrider, DOrcExecution, PeerLocation, ValueLocator, ValueLocatorFactory }
import orc.values.sites.JavaStaticMemberProxy
import orc.values.sites.JavaMemberProxy

/** A ValueLocator that maps VertexWithPathLen instances to a location base
  * on the vertex name.
  *
  * @author jthywiss
  */
class VertexValueLocator(execution: DOrcExecution) extends ValueLocator with CallLocationOverrider {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = execution.runtime.allLocations.size

  @inline
  private def locationFromVertexName(name: Int) = (name % numLocations - 1) + 1

  override val currentLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case v: VertexWithPathLen => Set(execution.locationForFollowerNum(locationFromVertexName(v.name)))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => Set(execution.locationForFollowerNum(locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name)))
  }

  override val valueIsLocal: PartialFunction[Any, Boolean] = {
    case v: VertexWithPathLen => locationFromVertexName(v.name) == execution.runtime.here.runtimeId
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name) == execution.runtime.here.runtimeId
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case v: VertexWithPathLen => Set(execution.locationForFollowerNum(locationFromVertexName(v.name)))
    case jmp: JavaMemberProxy if jmp.javaClass == classOf[VertexWithPathLen] && jmp.theObject != null => Set(execution.locationForFollowerNum(locationFromVertexName(jmp.theObject.asInstanceOf[VertexWithPathLen].name)))
  }

  override def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[VertexWithPathLen] && jsmp.memberName == "apply" => {
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"VertexWithPathLen.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val mustMigrate = locationFromVertexName(arguments(0).asInstanceOf[Number].intValue) != execution.runtime.here.runtimeId
        orc.run.distrib.porce.Logger.Invoke.finer("VertexValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy returning " + mustMigrate)
        Some(mustMigrate)
      }
      case _: orc.test.item.distrib.GraphOfVertexWithPathLen => Some(0 != execution.runtime.here.runtimeId)
      case jmp: JavaMemberProxy if jmp.javaClass == classOf[GraphOfVertexWithPathLen] => Some(0 != execution.runtime.here.runtimeId)
      case _ => {
        None
      }
    }
  }

  override def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[PeerLocation] = {
    target match {
      case jsmp: JavaStaticMemberProxy if jsmp.javaClass == classOf[VertexWithPathLen] && jsmp.memberName == "apply" => {
        orc.run.distrib.porce.Logger.Invoke.finer("VertexValueLocator.callLocationOverride JavaStaticMemberProxy case")
        require(arguments.length == 2 && arguments(0).isInstanceOf[Number], s"VertexWithPathLen.apply unexpected args: ${arguments.map(orc.util.GetScalaTypeName(_)).mkString(", ")}: ${arguments.mkString(", ")}")
        val loc = locationFromVertexName(arguments(0).asInstanceOf[Number].intValue)
        orc.run.distrib.porce.Logger.Invoke.finer("VertexValueLocator.callLocationMayNeedOverride JavaStaticMemberProxy callLocationOverride location:" + loc)
        Set(execution.locationForFollowerNum(loc))
      }
      case _: orc.test.item.distrib.GraphOfVertexWithPathLen => Set(execution.locationForFollowerNum(0))
      case jmp: JavaMemberProxy if jmp.javaClass == classOf[GraphOfVertexWithPathLen] => Set(execution.locationForFollowerNum(0))
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
class VertexValueLocatorFactory extends ValueLocatorFactory {
  def apply(execution: DOrcExecution): VertexValueLocator = new VertexValueLocator(execution)
}
