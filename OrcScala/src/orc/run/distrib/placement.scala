//
// placement.scala -- Scala traits AbstractLocation, ClusterLocations, MigrationDecision, and PinnedPlacementPolicy
// Project OrcScala
//
// Created by jthywiss on Sep 24, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** An opaque value that represents a distributed Orc runtime location.
  *
  * @author jthywiss
  */
trait AbstractLocation {}

/** Provides AbstractLocations representing this distributed Orc runtimes in
  * this distributed Orc cluster.
  *
  * @author jthywiss
  */
trait ClusterLocations[L <: AbstractLocation] {
  /* This interface will need to be widened to enable more uses of
   * DOrcPlacementPolicy */
  def allLocations: Set[L]
  def here: L
  def hereSet: Set[L]
}

/* Unused as of yet */
abstract sealed trait MigrationDecision
object MigrationDecision {
  case object Copy extends MigrationDecision
  case object Move extends MigrationDecision
  case object Remote extends MigrationDecision
}

/** Convenience DOrcPlacementPolicy that specifies that instances of this
  * class cannot be migrated.
  *
  * @author jthywiss
  */
trait PinnedPlacementPolicy extends DOrcPlacementPolicy {
  def permittedLocations[L <: AbstractLocation](locations: ClusterLocations[L]): Set[L] = locations.hereSet
}
