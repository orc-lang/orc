//
// LocationPinnedTuple.scala -- Scala classes LocationPinnedTupleConstructor and LocationPinnedTuple
// Project PorcE
//
// Created by jthywiss on Jun 27, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.Handle
import orc.error.OrcException
import orc.run.core.ExternalSiteCallHandle
import orc.values.OrcTuple
import orc.values.sites.Site

/** Superclass of Orc sites to construct LocationPinnedTuples
  *
  * @author jthywiss
  */
abstract class LocationPinnedTupleConstructor(locationNum: Int) extends Site with LocationPolicy {
  override def call(args: Array[AnyRef], h: Handle) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    try {
      h.publish(evaluate(args, h))
    } catch {
      case (e: OrcException) => h.halt(e)
    }
  }

  private def evaluate(args: Array[AnyRef], h: Handle) = {
    val loc = h.asInstanceOf[ExternalSiteCallHandle].caller.execution.asInstanceOf[DOrcExecution].locationForFollowerNum(locationNum)
    new LocationPinnedTuple(loc, args)
  }

  override def permittedLocations(runtime: DOrcRuntime) = Set(runtime.locationForRuntimeId(locationNum))
}

class Location0PinnedTupleConstructor extends LocationPinnedTupleConstructor(0) {}
class Location1PinnedTupleConstructor extends LocationPinnedTupleConstructor(1) {}
class Location2PinnedTupleConstructor extends LocationPinnedTupleConstructor(2) {}
class Location3PinnedTupleConstructor extends LocationPinnedTupleConstructor(3) {}
class Location4PinnedTupleConstructor extends LocationPinnedTupleConstructor(4) {}
class Location5PinnedTupleConstructor extends LocationPinnedTupleConstructor(5) {}
class Location6PinnedTupleConstructor extends LocationPinnedTupleConstructor(6) {}
class Location7PinnedTupleConstructor extends LocationPinnedTupleConstructor(7) {}
class Location8PinnedTupleConstructor extends LocationPinnedTupleConstructor(8) {}
class Location9PinnedTupleConstructor extends LocationPinnedTupleConstructor(9) {}
class Location10PinnedTupleConstructor extends LocationPinnedTupleConstructor(10) {}
class Location11PinnedTupleConstructor extends LocationPinnedTupleConstructor(11) {}
class Location12PinnedTupleConstructor extends LocationPinnedTupleConstructor(12) {}

/** An Orc tuple which has a location policy that only permits a given
  * location.
  *
  * @author jthywiss
  */
class LocationPinnedTuple(l: PeerLocation, args: Array[AnyRef]) extends OrcTuple(args) with LocationPolicy {
  private val permittedLocationSet = Set(l)
  override def permittedLocations(runtime: DOrcRuntime) = permittedLocationSet
}