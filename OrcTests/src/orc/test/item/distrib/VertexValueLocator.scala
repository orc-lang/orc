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
  }

  override val valueIsLocal: PartialFunction[Any, Boolean] = {
    case v: VertexWithPathLen => locationFromVertexName(v.name) == execution.runtime.here.runtimeId
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case v: VertexWithPathLen => Set(execution.locationForFollowerNum(locationFromVertexName(v.name)))
  }

  override def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target match {
      case c: Class[_] if c == classOf[VertexWithPathLen] => {
        require(arguments.length == 4 && arguments(0).isInstanceOf[Int])
        locationFromVertexName(arguments(0).asInstanceOf[Int]) != execution.runtime.here.runtimeId
      }
      case _ => false
    }
  }

  override def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[PeerLocation] = {
    target match {
      case c: Class[_] if c == classOf[VertexWithPathLen] => {
        require(arguments.length == 4 && arguments(0).isInstanceOf[Int])
        val loc = locationFromVertexName(arguments(0).asInstanceOf[Int])
        if (loc == execution.runtime.here.runtimeId) Set.empty else Set(execution.locationForFollowerNum(loc))
      }
      case _ => Set.empty
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
