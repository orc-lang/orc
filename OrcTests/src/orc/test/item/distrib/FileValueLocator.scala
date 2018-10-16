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

import orc.run.distrib.porce.{ CallLocationOverrider, DOrcExecution, DOrcRuntime, PeerLocation, ValueLocator, ValueLocatorFactory }

/** A ValueLocator that looks up java.io.File names in a filename-to-location
  * Map.
  *
  * @author jthywiss
  */
class FileValueLocator(execution: DOrcExecution) extends ValueLocator with CallLocationOverrider {
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
    case f: java.io.File if filenameLocationMap.contains(f.getName) => filenameLocationMap(f.getName) == execution.runtime.here.runtimeId
  }

  override val permittedLocations: PartialFunction[Any, Set[PeerLocation]] = {
    case f: java.io.File if filenameLocationMap.contains(f.getName) => Set(execution.locationForFollowerNum(filenameLocationMap(f.getName)))
  }

  def remoteLocationForFilePath(filePath: String): Option[DOrcRuntime#RuntimeId] = {
    val loc = filenameLocationMap.get(filePath.split(java.io.File.separatorChar).last)
    if (loc.isEmpty) None else Some(loc.get)
  }

  override def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean] = {
    target match {
      case c: Class[_] if c == classOf[java.io.File] && arguments.length == 1 && arguments(0).isInstanceOf[String] => {
        remoteLocationForFilePath(arguments(0).asInstanceOf[String]).map(_ != execution.runtime.here.runtimeId)
      }
      case _ => None
    }
  }

  override def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[PeerLocation] = {
    target match {
      case c: Class[_] if c == classOf[java.io.File] && arguments.length == 1 && arguments(0).isInstanceOf[String] => {
        val loc = remoteLocationForFilePath(arguments(0).asInstanceOf[String])
        if (loc.isEmpty) Set.empty else Set(execution.locationForFollowerNum(loc.get))
      }
      case _ => Set.empty
    }
  }

}

/** Service provider to get FileValueLocator instance.
  *
  * @author jthywiss
  */
class FileValueLocatorFactory extends ValueLocatorFactory {
  def apply(execution: DOrcExecution): FileValueLocator = new FileValueLocator(execution)
}
