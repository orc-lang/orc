//
// Add.scala -- Scala class Add
// Project OrcScala
//
// Created by amp on Sept 25, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import orc.values.sites._
import orc.values.OrcRecord
import orc.Invoker
import orc.values.sites.IllegalArgumentInvoker

object Add extends OverloadedDirectInvokerMethod2[Any, Any] with FunctionalSite {
  def getInvokerSpecialized(arg1: Any, arg2: Any) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: Number, b: Number) =>
        (a, b) match {
          case (a: BigDecimal, b: BigDecimal) =>
            invoker(a, b)(_ + _)
          case (a: java.lang.Double, b: java.lang.Double) =>
            invoker(a, b)(_.doubleValue() + _.doubleValue())
          case (a: java.math.BigDecimal, b: java.math.BigDecimal) =>
            invoker(a, b)(_.add(_))
          case (a: java.lang.Long, b: java.lang.Long) =>
            invoker(a, b)(_.longValue() + _.longValue())
          case (a: java.lang.Integer, b: java.lang.Integer) =>
            invoker(a, b)(_.intValue() + _.intValue())
          case (a: BigInt, b: BigInt) =>
            invoker(a, b)(_ + _)
          case (a: java.math.BigInteger, b: java.math.BigInteger) =>
            invoker(a, b)(_.add(_))
          case (a: BigDecimal, b: Number) =>
            invoker(a, b)(_ + _.doubleValue())
          case (a: Number, b: BigDecimal) =>
            invoker(a, b)(_.doubleValue() + _)
          case (a: java.lang.Double, b: Number) =>
            invoker(a, b)(_.doubleValue() + _.doubleValue())
          case (a: Number, b: java.lang.Double) =>
            invoker(a, b)(_.doubleValue() + _.doubleValue())
          case (a: BigInt, b: Number) =>
            invoker(a, b)(_ + _.longValue())
          case (a: Number, b: BigInt) =>
            invoker(a, b)(_.longValue() + _)
          case (a: java.lang.Long, b: Number) =>
            invoker(a, b)(_.longValue() + _.longValue())
          case (a: Number, b: java.lang.Long) =>
            invoker(a, b)(_.longValue() + _.longValue())
          case (a: Number, b: Number) =>
            IllegalArgumentInvoker(this, Array(a, b))
        }
      case (a: String, b: String) =>
        invoker(a, b)((a, b) => a + b)
      case (a: String, b) =>
        invokerStaticType(a, b)((a, b) => a + orc.values.Format.formatValue(b, false))
      case (a, b: String) =>
        invokerStaticType(a, b)((a, b) => orc.values.Format.formatValue(a, false) + b)
      case (a: OrcRecord, b: OrcRecord) =>
        invoker(a, b)(_ extendWith _)
      case (a, b) =>
        IllegalArgumentInvoker(this, Array(a.asInstanceOf[AnyRef], b.asInstanceOf[AnyRef]))
    }
  }
  
  override def toString = "Add"
}