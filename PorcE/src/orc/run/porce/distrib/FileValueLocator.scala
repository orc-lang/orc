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

package orc.run.porce.distrib

/** A ValueLocator that looks up java.io.File names in a filename-to-location
  * Map.
  *
  * @author jthywiss
  */
class FileValueLocator(execution: DOrcExecution) extends ValueLocator {
  val filenameLocationMap = Map(
      "adventure-1.txt" -> 1,
      "adventure-2.txt" -> 2,
      "adventure-3.txt" -> 3,
      "adventure-4.txt" -> 4,
      "adventure-5.txt" -> 5,
      "adventure-6.txt" -> 6,
      "adventure-7.txt" -> 7,
      "adventure-8.txt" -> 8,
      "adventure-9.txt" -> 9,
      "adventure-10.txt" -> 10,
      "adventure-11.txt" -> 11,
      "adventure-12.txt" -> 12
      )

  override val currentLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

}
