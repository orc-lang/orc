//
// CeilFloor.scala -- Scala objects Ceil and Floor
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.Invoker
import orc.values.sites.{ FunctionalSite, OverloadedDirectInvokerMethod1 }
import java.math.{ BigDecimal => JBigDecimal, BigInteger => JBigInteger }

object UMinus extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number) = {
    arg1 match {
      case a: java.lang.Double => invoker(a)(a => -a.doubleValue())
      case a: java.lang.Float => invoker(a)(a => -a.floatValue())
      case a: java.lang.Long => invoker(a)(a => -a.longValue())
      case a: java.lang.Integer => invoker(a)(a => -a.intValue())
      case a: java.lang.Byte => invoker(a)(a => -a.byteValue())
      case a: java.lang.Short => invoker(a)(a => -a.shortValue())
      case a: BigDecimal => invoker(a)(a => { -a })
      case a: JBigDecimal => invoker(a)(a => { a.negate() })
      case a: BigInt => invoker(a)(a => { -a })
      case a: JBigInteger => invoker(a)(a => { a.negate() })
      case a: Number => invokerStaticType(a)(a => a)
    }
  }
  override def toString = "Ceil"
}
