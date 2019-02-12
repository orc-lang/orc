//
// LocationMap.scala -- Scala class LocationMap
// Project OrcScala
//
// Created by jthywiss on Feb 10, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

/** A RuntimeId to RuntimeRef map.  Needed instead of
  * scala.collection.concurrent.Map to provide bulk operation semantics with
  * concurrent updates. This just implements the few operations we need
  * here.  Also notifies waiters upon any changes.
  */
class LocationMap[RuntimeId, Location](here: Location) {
  protected val theMap = scala.collection.mutable.Map[RuntimeId, Location]()
  protected var cachedLocationSnapshot: Option[scala.collection.immutable.Set[Location]] = None
  protected var cachedOtherLocationsSnapshot: Option[scala.collection.immutable.Set[Location]] = None

  def size: Int = synchronized { theMap.size }

  def apply(id: RuntimeId): Location = synchronized { theMap(id) }

  def put(id: RuntimeId, loc: Location): Option[Location] = synchronized {
    val r = theMap.put(id, loc)
    invalidateCaches()
    notifyAll()
    r
  }

  def putIfAbsent(id: RuntimeId, loc: Location): Option[Location] = synchronized {
    val existingLoc = theMap.get(id)
    if (existingLoc.isEmpty) {
      theMap.put(id, loc)
      invalidateCaches()
      notifyAll()
    }
    existingLoc
  }

  def remove(id: RuntimeId): Option[Location] = synchronized {
    val r = theMap.remove(id)
    invalidateCaches()
    notifyAll()
    r
  }

  def locationSnapshot: scala.collection.immutable.Set[Location] = synchronized {
    if (cachedLocationSnapshot.isEmpty) {
      cachedLocationSnapshot = Some(theMap.values.toSet)
    }
    cachedLocationSnapshot.get
  }

  def otherLocationsSnapshot: scala.collection.immutable.Set[Location] = synchronized {
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
