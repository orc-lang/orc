//
// CeilFloor.scala -- Scala objects Ceil and Floor
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import java.math.{ BigDecimal => JBigDecimal, BigInteger => JBigInteger }

import orc.values.sites.{ FunctionalSite, LocalSingletonSite, OverloadedDirectInvokerMethod1 }

object UMinus extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite with Serializable with LocalSingletonSite {
  def getInvokerSpecialized(arg1: Number) = {
    arg1 match {
      case a: java.lang.Double => invokerInline(a)(a => -a.doubleValue())
      case a: java.lang.Float => invokerInline(a)(a => -a.floatValue())
      case a: java.lang.Long => invokerInline(a)(a => -a.longValue())
      case a: java.lang.Integer => invokerInline(a)(a => -a.intValue())
      case a: java.lang.Byte => invokerInline(a)(a => -a.byteValue())
      case a: java.lang.Short => invokerInline(a)(a => -a.shortValue())
      case a: BigDecimal => invoker(a)(a => { -a })
      case a: JBigDecimal => invoker(a)(a => { a.negate() })
      case a: BigInt => invoker(a)(a => { -a })
      case a: JBigInteger => invoker(a)(a => { a.negate() })
      case a: Number => invokerStaticType(a)(a => a)
    }
  }
  override def toString = "Ceil"
}
