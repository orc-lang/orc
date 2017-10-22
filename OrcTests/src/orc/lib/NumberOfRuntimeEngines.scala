//
// NumberOfRuntimeEngines.scala -- Scala object NumberOfRuntimeEngines
// Project OrcTests
//
// Created by jthywiss on Oct 6, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib

import orc.CallContext
import orc.run.core.CallController
import orc.run.distrib.ClusterLocations
import orc.run.porce.runtime.CPSCallContext
import orc.types.IntegerType
import orc.values.sites.{ FunctionalSite, Range, Site0, TypedSite }

/** Orc site that returns the number of runtime engines in the current cluster.
  *
  * @author jthywiss
  */
object NumberOfRuntimeEngines extends Site0 with FunctionalSite with TypedSite {

  def call(callContext: CallContext): Unit = {
    val numRE = callContext match {
      case cc: CallController => cc.caller.runtime match {
        case cl: ClusterLocations[_] => cl.allLocations.size
        case _ => 1
      }
      case cc: CPSCallContext => cc.runtime match {
        case cl: ClusterLocations[_] => cl.allLocations.size
        case _ => 1
      }
      case _ => 0
    }
    callContext.publish(BigDecimal.valueOf(numRE))
  }

  override def orcType() = IntegerType

  override def publications = Range(0, 1)
}
