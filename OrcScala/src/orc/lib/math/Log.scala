//
// Log.scala -- Scala object Log
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.values.sites.{ FunctionalSite, LocalSingletonSite, OverloadedDirectInvokerMethod1 }


object Log extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite with Serializable with LocalSingletonSite {
  def getInvokerSpecialized(arg1: Number) = {
    invokerInline(arg1)(a => Math.log(a.doubleValue()))
  }
  override def toString = "Log"
}
