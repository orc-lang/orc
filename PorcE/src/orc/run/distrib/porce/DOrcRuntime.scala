//
// DOrcRuntime.scala -- Scala class DOrcRuntime
// Project PorcE
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

import orc.ast.porc.MethodCPS
import orc.run.distrib.ClusterLocations
import orc.run.porce.runtime.PorcERuntime

/** Distributed Orc (dOrc) Runtime Engine.
  *
  * Rule of thumb: Orc Runtimes manage external interaction, with the
  * environment. Program state and engine-internal behavior is the bailiwick
  * of Orc Executions.
  *
  * @author jthywiss
  */
abstract class DOrcRuntime(val runtimeId: DOrcRuntime#RuntimeId, engineInstanceName: String)
  extends PorcERuntime(engineInstanceName, null)
  with ClusterLocations[PeerLocation] {

  type ProgramAST = MethodCPS

  /* For now, runtime IDs and Execution follower numbers are the same.  When
   * we host more than one execution in an engine, they will be different. */

  type RuntimeId = Int

  def locationForRuntimeId(runtimeId: DOrcRuntime#RuntimeId): PeerLocation

  override def allLocations: Set[PeerLocation]

  override def here: PeerLocation
  override def hereSet: Set[PeerLocation]

  /** A thread ID 32-bit integer that can be combined with a thread local
    * counter to produce identifiers.
    *
    * WARNING: Uniqueness is attempted, but not guaranteed.  Indicative only,
    * for non-critical uses, such as debugging log/trace.
    *
    * We use the least significant 8 bits of the runtime number and the
    * least significant 24 bits of Java's thread ID.
    */
  override def runtimeDebugThreadId: Int = runtimeId << 24 | Thread.currentThread().getId.asInstanceOf[Int]

}

/** A RuntimeId to Location map.  Needed instead of
  * scala.collection.concurrent.Map to provide bulk operation semantics with
  * concurrent updates. This just implements the few operations we need
  * here.  Also notifies waiters upon any changes.
  */
class LocationMap[L](here: L) {
  protected val theMap = scala.collection.mutable.Map[DOrcRuntime#RuntimeId, L]()
  protected var cachedLocationSnapshot: Option[scala.collection.immutable.Set[L]] = None
  protected var cachedOtherLocationsSnapshot: Option[scala.collection.immutable.Set[L]] = None

  def size: Int = synchronized { theMap.size }

  def apply(id: DOrcRuntime#RuntimeId): L = synchronized { theMap(id) }

  def put(id: DOrcRuntime#RuntimeId, loc: L): Option[L] = synchronized {
    val r = theMap.put(id, loc)
    invalidateCaches()
    notifyAll()
    r
  }

  def putIfAbsent(id: DOrcRuntime#RuntimeId, loc: L): Option[L] = synchronized {
    val existingLoc = theMap.get(id)
    if (existingLoc.isEmpty) {
      theMap.put(id, loc)
      invalidateCaches()
      notifyAll()
    }
    existingLoc
  }

  def remove(id: DOrcRuntime#RuntimeId): Option[L] = synchronized {
    val r = theMap.remove(id)
    invalidateCaches()
    notifyAll()
    r
  }

  def locationSnapshot: scala.collection.immutable.Set[L] = synchronized {
    if (cachedLocationSnapshot.isEmpty) {
      cachedLocationSnapshot = Some(theMap.values.toSet)
    }
    cachedLocationSnapshot.get
  }

  def otherLocationsSnapshot: scala.collection.immutable.Set[L] = synchronized {
    if (cachedOtherLocationsSnapshot.isEmpty) {
      cachedOtherLocationsSnapshot = Some(theMap.values.filterNot({ _ == here }).toSet)
    }
    cachedOtherLocationsSnapshot.get
  }

  protected def invalidateCaches(): Unit = {
    cachedLocationSnapshot = None
    cachedOtherLocationsSnapshot = None
  }
}
