//
// ValueLocator.scala -- Scala traits ValueLocator, Location, LocationPolicy, and MigrationDecision
// Project PorcE
//
// Created by jthywiss on Dec 28, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

/** Given an Orc value, find the locations for that value.
  *
  * @author jthywiss
  */
trait ValueLocator {
  val currentLocations: PartialFunction[Any, Set[PeerLocation]]
  val permittedLocations: PartialFunction[Any, Set[PeerLocation]]
}

/** A Distributed Orc runtime participating in this cluster of runtimes.
  * Locations can be sent commands.
  *
  * @author jthywiss
  */
trait Location[-M <: OrcCmd] {
  def send(message: M): Unit
  def sendInContext(execution: DOrcExecution)(message: M): Unit
  def runtimeId: DOrcRuntime#RuntimeId
}

/** Provides the set of Locations where this value may feasibly reside.
  *
  * @author jthywiss
  */
trait LocationPolicy {
  def permittedLocations(runtime: DOrcRuntime): Set[PeerLocation]
}

abstract sealed trait MigrationDecision
object MigrationDecision {
  case object Copy extends MigrationDecision
  case object Move extends MigrationDecision
  case object Remote extends MigrationDecision
}
