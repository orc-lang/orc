//
// ValueLocator.scala -- Scala traits ValueLocator and Location
// Project PorcE
//
// Created by jthywiss on Dec 28, 2015.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.run.distrib.AbstractLocation

/** Given an Orc value, find the locations for that value.
  *
  * @author jthywiss
  */
trait ValueLocator {
  val currentLocations: PartialFunction[Any, Set[PeerLocation]]
  val valueIsLocal: PartialFunction[Any, Boolean]
  val permittedLocations: PartialFunction[Any, Set[PeerLocation]]
}

trait CallLocationOverrider extends ValueLocator {
  def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]) : Boolean
  def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[PeerLocation]
}

/** Service provider to get ValueLocator instances for a DOrcExecution.
  *
  * @see java.util.ServiceLoader
  * @author jthywiss
  */
trait ValueLocatorFactory {
  def apply(execution: DOrcExecution): ValueLocator
}

/** A Distributed Orc runtime participating in this cluster of runtimes.
  * Locations can be sent commands.
  *
  * @author jthywiss
  */
trait Location[-M <: OrcCmd] extends AbstractLocation {
  def send(message: M): Unit
  def sendInContext(execution: DOrcExecution)(message: M): Unit
  def runtimeId: DOrcRuntime#RuntimeId
}
