//
// NoLocationAvailable.scala -- Scala class NoLocationAvailable
// Project OrcScala
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import orc.error.runtime.TokenException

/** Exception thrown when no allowed common location can be found to execute a dOrc operation.
  *
  * @author jthywiss
  */
class NoLocationAvailable(valLocations: Seq[(AnyRef, Set[Long])])
  extends TokenException("No common location for: " + valLocations.map(p => s"${p._1}: ${orc.util.GetScalaTypeName(p._1)} at ${p._2.map(_.toHexString).mkString(", ")}").mkString("; "))
