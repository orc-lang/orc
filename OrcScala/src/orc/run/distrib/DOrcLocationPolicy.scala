//
// DOrcLocationPolicy.scala -- Scala traits DOrcLocationPolicy, PinnedLocationPolicy, AbstractLocation, ClusterLocations and MigrationDecision
// Project OrcScala
//
// Created by jthywiss on Sep 24, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** Provides the set of Locations where this value may feasibly reside.
  *
  * @author jthywiss
  */
trait DOrcLocationPolicy[L <: AbstractLocation] {
  def permittedLocations(locations: ClusterLocations[L]): Set[L]
}

/** Convenience DOrcLocationPolicy that specifies that instances of this
  * class cannot be migrated.
  *
  * @author jthywiss
  */
trait PinnedLocationPolicy[L <: AbstractLocation] {
  def permittedLocations(locations: ClusterLocations[L]): Set[L] = locations.hereSet
}

/** An opaque value that represents a distributed Orc runtime location.
  *
  * @author jthywiss
  */
trait AbstractLocation { }

/** Provides AbstractLocations representing this distributed Orc runtimes in
  * this distributed Orc cluster.
  *
  * @author jthywiss
  */
trait ClusterLocations[L <: AbstractLocation] {
  /* This interface will need to be widened to enable more uses of
   * DOrcLocationPolicy */
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
