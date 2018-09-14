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

import orc.run.distrib.ClusterLocations
import orc.types.IntegerType
import orc.values.NumericsConfig
import orc.values.sites.{ TypedSite, FunctionalSite, Range, Site0Base }
import orc.{ OrcRuntime, Invoker }

/** Orc site that returns the number of runtime engines in the current cluster.
  *
  * @author jthywiss
  */
object NumberOfRuntimeEngines extends Site0Base with FunctionalSite with TypedSite {

  def getInvoker(runtime: OrcRuntime): Invoker = {
    invoker(this) { (ctx, target) =>
      val numRE: Int = ctx.runtime match {
        case cl: ClusterLocations[_] => cl.allLocations.size
        case _ => 1
      }
      ctx.publish(NumericsConfig.toOrcIntegral(numRE))
    }
  }

  override def orcType() = IntegerType

  override def publications = Range(0, 1)
}
