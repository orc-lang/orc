//
// placement.scala -- Scala traits AbstractLocation, ClusterLocations, MigrationDecision, and PinnedPlacementPolicy
// Project OrcScala
//
// Created by jthywiss on Sep 24, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
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

/** Provides AbstractLocations representing the distributed Orc runtimes in
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
  /* Designating a "leader" runtime may eventually go away. */
  def leader: L
  def leaderSet: Set[L]
}

/* See DOrcPlacementPolicy.java for the DOrcPlacementPolicy interface */

/** Convenience DOrcPlacementPolicy that specifies that instances of this
  * class cannot be migrated.
  *
  * @author jthywiss
  */
trait PinnedPlacementPolicy extends DOrcPlacementPolicy {
  def permittedLocations[L <: AbstractLocation](locations: ClusterLocations[L]): Set[L] = locations.hereSet
}

/** Given an Orc value, find the locations for that value.  Multiple
  * ValueLocator services may exist per execution, each handling some subset
  * of Orc values.  A ValueLocator instance's currentLocations/valueIsLocal/
  * permittedLocations partial functions must be defined on exactly the
  * values that this ValueLocator is able to provide answers for.  The d-Orc
  * execution composes the ValueLocator services when locating values.  See
  * ValueLocatorFactory for configuration and instantiation.
  *
  * @see ValueLocatorFactory
  * @author jthywiss
  */
trait ValueLocator[L <: AbstractLocation] {
  val currentLocations: PartialFunction[Any, Set[L]]
  val valueIsLocal: PartialFunction[Any, Boolean]
  val permittedLocations: PartialFunction[Any, Set[L]]
}

/** An extended ValueLocator that also specifies the location for certain
  * site calls.  This is useful for ensuring constructor calls are migrated
  * to their preferred location.
  *
  * @author jthywiss
  */
trait CallLocationOverrider[L <: AbstractLocation] extends ValueLocator[L] {
  def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean]
  def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[L]
}

/** Service provider to get ValueLocator instances for an execution.  Each
  * registered class will be called once per execution, to create the
  * associated ValueLocator instance.  Register implementing classes as
  * orc.run.distrib.ValueLocatorFactory service providers (in your JAR's
  * META-INF directory).
  *
  * @see java.util.ServiceLoader
  * @author jthywiss
  */
trait ValueLocatorFactory[L <: AbstractLocation] {
  def apply(locations: ClusterLocations[L]): ValueLocator[L]
}

/* Unused as of yet */
abstract sealed trait MigrationDecision
object MigrationDecision {
  case object Copy extends MigrationDecision
  case object Move extends MigrationDecision
  case object Remote extends MigrationDecision
}
