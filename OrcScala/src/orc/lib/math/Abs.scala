//
// Abs.scala -- Scala object Abs
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.Invoker
import orc.values.sites.{ FunctionalSite, IllegalArgumentInvoker, LocalSingletonSite, OverloadedDirectInvokerMethod1 }

object Abs extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite with Serializable with LocalSingletonSite {
  def getInvokerSpecialized(arg1: Number): Invoker = {
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
