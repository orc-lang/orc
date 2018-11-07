//
// Sub.scala -- Scala object Sub
// Project OrcScala
//
// Created by amp on Sept 25, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.values.sites.{ FunctionalSite, IllegalArgumentInvoker, LocalSingletonSite, OverloadedDirectInvokerMethod2 }

object Sub extends OverloadedDirectInvokerMethod2[Number, Number] with FunctionalSite with Serializable with LocalSingletonSite {
  def getInvokerSpecialized(arg1: Number, arg2: Number) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)(_ - _)
      case (a: java.lang.Double, b: java.lang.Double) =>
        invokerInline(a, b)(_.doubleValue() - _.doubleValue())
      case (a: java.math.BigDecimal, b: java.math.BigDecimal) =>
        invoker(a, b)(_.subtract(_))
      case (a: java.lang.Long, b: java.lang.Long) =>
        invokerInline(a, b)(_.longValue() - _.longValue())
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invokerInline(a, b)(_.intValue() - _.intValue())
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)(_ - _)
      case (a: java.math.BigInteger, b: java.math.BigInteger) =>
        invoker(a, b)(_.subtract(_))
      case (a: BigDecimal, b: Number) =>
        invoker(a, b)(_ - _.doubleValue())
      case (a: Number, b: BigDecimal) =>
        invoker(a, b)(_.doubleValue() - _)
      case (a: java.lang.Double, b: Number) =>
        invoker(a, b)(_.doubleValue() - _.doubleValue())
      case (a: Number, b: java.lang.Double) =>
        invoker(a, b)(_.doubleValue() - _.doubleValue())
      case (a: BigInt, b: Number) =>
        invoker(a, b)(_ - _.longValue())
      case (a: Number, b: BigInt) =>
        invoker(a, b)(_.longValue() - _)
      case (a: java.lang.Long, b: Number) =>
        invoker(a, b)(_.longValue() - _.longValue())
      case (a: Number, b: java.lang.Long) =>
        invoker(a, b)(_.longValue() - _.longValue())
      case (a: Number, b: Number) =>
        IllegalArgumentInvoker(this, Array(a, b))
    }
  }
  override def toString = "Sub"
}
