//
// FileValueLocator.scala -- Scala class FileValueLocator
// Project OrcTests
//
// Created by jthywiss on Sep 20, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.run.distrib.porce.{ DOrcExecution, PeerLocation, ValueLocator, ValueLocatorFactory }

/** A ValueLocator that looks up java.io.File names in a filename-to-location
  * Map.
  *
  * @author jthywiss
  */
class FileValueLocator(execution: DOrcExecution) extends ValueLocator {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = execution.runtime.allLocations.size
  /* Map files to locations 1 ... n-1, skipping leader (0) */
  val filenameLocationMap =
    /* wordcount-input-data files */
    1.to(120).map(i => (s"input-copy-$i.txt", (i % numLocations - 1) + 1)).toMap ++
    /* holmes_test_data files */
    1.to(12).map(i => (s"adventure-$i.txt", (i % numLocations - 1) + 1)).toMap

  override val currentLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

  override val valueIsLocal: PartialFunction[Any, Boolean] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => filenameLocationMap(f.getName) != execution.runtime.here.runtimeId
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

}

/** Service provider to get FileValueLocator instance.
  *
  * @author jthywiss
  */
class FileValueLocatorFactory extends ValueLocatorFactory {
  def apply(execution: DOrcExecution): FileValueLocator = new FileValueLocator(execution)
}
