//
// ValueLocator.scala -- Scala traits ValueLocator and Location
// Project OrcScala
//
// Created by jthywiss on Dec 28, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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
  def here: Location
  def currentLocations(v: Any): Set[Location]
  def permittedLocations(v: Any): Set[Location]
}

trait Location {
  def send(message: OrcPeerCmd)
}

trait LocationPolicy {
  def permittedLocations(): Set[Location]
}
