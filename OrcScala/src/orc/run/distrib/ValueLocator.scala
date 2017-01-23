//
// ValueLocator.scala -- Scala traits ValueLocator and Location
// Project OrcScala
//
// Created by jthywiss on Dec 28, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

/** Given an Orc value, find the locations for that value.
  *
  * @author jthywiss
  */
trait ValueLocator {
  def currentLocations(v: Any): Set[PeerLocation]
  def permittedLocations(v: Any): Set[PeerLocation]
}

/** A Distributed Orc runtime participating in this cluster of runtimes.
  * Locations can be sent commands.
  *
  * @author jthywiss
  */
trait Location[-M <: OrcCmd] {
  def send(message: M)
}

/** Provides the set of Locations that can feasibly have a copy of this value.
  *
  * @author jthywiss
  */
trait LocationPolicy {
  def permittedLocations(): Set[PeerLocation]
}

trait MigrationDecision
case object Copy extends MigrationDecision
case object Move extends MigrationDecision
case object Remote extends MigrationDecision

/** Orc values implementing this trait will be notified of marshalling for
  * serialization to another location.
  *
  * @author jthywiss
  */
trait DOrcMarshallingNotifications {
  def marshalled() {}
  def unmarshalled() {}
}
