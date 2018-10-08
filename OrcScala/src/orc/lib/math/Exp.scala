//
// Log2.scala -- Scala object Log2
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.values.sites.{ FunctionalSite, OverloadedDirectInvokerMethod1 }

object Exp extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number) = {
    invoker(arg1)(a => Math.exp(a.doubleValue()))
  }
  override def toString = "Exp"
}
