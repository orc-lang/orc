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

import orc.run.distrib.porce.{ DOrcExecution, PeerLocation, ValueLocator, ValueLocatorFactory }

/** A ValueLocator that maps VertexWithPathLen instances to a location base
  * on the vertex name.
  *
  * @author jthywiss
  */
class VertexValueLocator(execution: DOrcExecution) extends ValueLocator {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = execution.runtime.allLocations.size

  override val currentLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case v: VertexWithPathLen => Set(execution.locationForFollowerNum((v.name % numLocations - 1) + 1))
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case v: VertexWithPathLen => Set(execution.locationForFollowerNum((v.name % numLocations - 1) + 1))
  }

}

/** Service provider to get VertexValueLocator instance.
  *
  * @author jthywiss
  */
class VertexValueLocatorFactory extends ValueLocatorFactory {
  def apply(execution: DOrcExecution): VertexValueLocator = new VertexValueLocator(execution)
}
