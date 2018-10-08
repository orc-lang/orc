//
// Abs.scala -- Scala object Abs
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.values.sites.{ FunctionalSite, OverloadedDirectInvokerMethod1, IllegalArgumentInvoker }

object Abs extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number) = {
    arg1 match {
      case a: java.lang.Double => invoker(a)(a => Math.abs(a.doubleValue()))
      case a: java.lang.Integer => invoker(a)(a => Math.abs(a.intValue()))
      case a: java.lang.Long => invoker(a)(a => Math.abs(a.longValue()))
      case a: BigDecimal => invoker(a)(a => a.abs)
      case a: BigInt => invoker(a)(a => a.abs)
      case a: java.math.BigDecimal => invoker(a)(a => a.abs)
      case a: java.math.BigInteger => invoker(a)(a => a.abs)
      case a: Number =>
        IllegalArgumentInvoker(this, Array(a))
    }
  }

  override def toString = "Abs"
}
