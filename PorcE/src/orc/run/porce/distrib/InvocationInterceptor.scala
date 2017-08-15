//
// SupportForDOrc.scala -- Scala traits DistributionSupport, SupportForNondistributedOrc, and SupportForDOrc
// Project PorcE
//
// Created by jthywiss on Dec 21, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

import orc.run.porce.runtime.{ CallRecord, PorcEExecution }

/** Adds facilities for distributed Orc calls to an Orc execution.
  *
  * @author amp
  */
trait DistributionSupport {
  this: PorcEExecution =>

  /** Return true iff the invocation is distributed.
    *
    * This call must be as fast as possible since it is called before every external call.
    */
  def isDistributedInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean

  /** Invoke an call remotely as needed by the target and arguments.
    *
    * This will only be called if `isDistributedInvocation(target, arguments)` is true.
    */
  def invokeDistributed(callRecord: CallRecord, target: AnyRef, arguments: Array[AnyRef]): Unit
}

/** Implement no distributed calls at all.
  *
  * @author amp
  */
trait SupportForNondistributedOrc extends DistributionSupport {
  this: PorcEExecution =>

  override def isDistributedInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = false
  override def invokeDistributed(callRecord: CallRecord, target: AnyRef, arguments: Array[AnyRef]): Unit = ???
}

/** Adds real implementations of distributed Orc calls to an Orc execution.
  *
  * @author jthywiss
  * @author amp
  */
trait SupportForDOrc extends DistributionSupport {
  this: DOrcExecution =>

  override def isDistributedInvocation(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    // WARNING: Contains return!!!

    // TODO: PERFORMANCE: This would probably gain a lot by specializing on the number of arguments. That will probably require a simpler structure for the loops.
    if (target.isInstanceOf[RemoteRef] || arguments.exists(_.isInstanceOf[RemoteRef])) {
      return true
    } else {
      val here = runtime.here
      for (v <- arguments.view :+ target) {
        if (v.isInstanceOf[LocationPolicy] && !currentLocations(v).contains(here)) {
          return true
        }
      }

      return false
    }
  }

  override def invokeDistributed(callRecord: CallRecord, target: AnyRef, arguments: Array[AnyRef]) {
    def pickLocation(ls: Set[PeerLocation]) = ls.head

    //Logger.entering(getClass.getName, "invokeDistributed", Seq(target.getClass.getName, target, arguments))

    // TODO: If this every turns out to be a performance issue I suspect a bloom-filter-optimized set would help.
    val intersectLocs = (arguments map currentLocations).fold(currentLocations(target)) { _ & _ }
    require(!(intersectLocs contains runtime.here))
    orc.run.distrib.Logger.finest(s"siteCall($target,$arguments): intersection of current locations=$intersectLocs")
    val candidateDestinations = {
      if (intersectLocs.nonEmpty) {
        intersectLocs
      } else {
        val intersectPermittedLocs = (arguments map permittedLocations).fold(permittedLocations(target)) { _ & _ }
        if (intersectPermittedLocs.nonEmpty) {
          intersectPermittedLocs
        } else {
          throw new NoLocationAvailable(target +: arguments.toSeq)
        }
      }
    }
    orc.run.distrib.Logger.finest(s"candidateDestinations=$candidateDestinations")
    val destination = pickLocation(candidateDestinations)
    sendCall(callRecord, target, arguments, destination)
  }
}
