//
// FileValueLocator.scala -- Scala class FileValueLocator
// Project PorcE
//
// Created by jthywiss on Sep 20, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.porce

/** A ValueLocator that looks up java.io.File names in a filename-to-location
  * Map.
  *
  * @author jthywiss
  */
class FileValueLocator(execution: DOrcExecution) extends ValueLocator {
  /* Assuming follower numbers are contiguous from 0 (leader) to n-1 */
  private val numLocations = execution.runtime.allLocations.size
  val filenameLocationMap = Seq(
      "adventure-1.txt",
      "adventure-2.txt",
      "adventure-3.txt",
      "adventure-4.txt",
      "adventure-5.txt",
      "adventure-6.txt",
      "adventure-7.txt",
      "adventure-8.txt",
      "adventure-9.txt",
      "adventure-10.txt",
      "adventure-11.txt",
      "adventure-12.txt"
    ).zipWithIndex.map({
      /* Map files to locations 1 ... n-1, skipping leader (0) */
      case (fn, i) => (fn, (i % numLocations - 1) + 1)
    }).toMap

  override val currentLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

}
