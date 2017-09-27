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
import java.math.{ BigDecimal => JBigDecimal }

object Ceil extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number): Invoker = {
    arg1 match {
      case a: java.lang.Double => invoker(a)(a => BigDecimal(Math.ceil(a)).toBigInt())
      case a: java.lang.Float => invoker(a)(a => BigDecimal(Math.ceil(a.toDouble)).toBigInt())
      case a: BigDecimal => invoker(a)(a => {
        BigInt(a.bigDecimal.setScale(0, JBigDecimal.ROUND_CEILING).toBigInteger())
      })
      case a: JBigDecimal => invoker(a)(a => {
        BigInt(a.setScale(0, JBigDecimal.ROUND_CEILING).toBigInteger())
      })
      case a: Number => invokerStaticType(a)(a => a)
    }
  }
}

object Floor extends OverloadedDirectInvokerMethod1[Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number): Invoker = {
    arg1 match {
      case a: java.lang.Double => invoker(a)(a => BigDecimal(Math.floor(a)).toBigInt())
      case a: java.lang.Float => invoker(a)(a => BigDecimal(Math.floor(a.toDouble)).toBigInt())
      case a: BigDecimal => invoker(a)(a => {
        BigInt(a.bigDecimal.setScale(0, JBigDecimal.ROUND_FLOOR).toBigInteger())
      })
      case a: JBigDecimal => invoker(a)(a => {
        BigInt(a.setScale(0, JBigDecimal.ROUND_FLOOR).toBigInteger())
      })
      case a: Number => invokerStaticType(a)(a => a)
    }
  }
}
