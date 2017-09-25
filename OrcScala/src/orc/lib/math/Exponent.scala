//
// Exponent.scala -- Scala class Exponent
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
import orc.Invoker
import orc.IllegalArgumentInvoker

object Exponent extends OverloadedDirectInvokerMethod2[Number, Number] with FunctionalSite {
  def getInvokerSpecialized(arg1: Number, arg2: Number): Invoker = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigInt) =>
        invoker(a, b)((a, b) => {
          if (b.isValidInt) {
            a pow b.intValue()
          } else {
            throw new ArithmeticException("Exponent out of range");
          }
        })
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: java.lang.Double, b: java.lang.Double) => 
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: java.lang.Long, b: java.lang.Long) => 
        invoker(a, b)((a, b) => math.pow(a.longValue(), b.longValue()))
      case (a: java.lang.Integer, b: java.lang.Integer) => 
        invoker(a, b)((a, b) => math.pow(a.intValue(), b.intValue()))
      case (a: BigInt, b: BigInt) => 
        invoker(a, b)((a, b) => {
          if (b.isValidInt) {
            a pow b.intValue()
          } else {
            throw new ArithmeticException("Exponent out of range");
          }
        })
      case (a: BigDecimal, b: Number) => 
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: Number, b: BigDecimal) => 
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: java.lang.Double, b: Number) =>
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: Number, b: java.lang.Double) => 
        invoker(a, b)((a, b) => math.pow(a.doubleValue(), b.doubleValue()))
      case (a: Number, b: Number) =>
        IllegalArgumentInvoker(this, Array(a, b))
    }
  }
}