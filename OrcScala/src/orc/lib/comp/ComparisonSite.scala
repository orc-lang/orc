//
// ComparisonSite.scala -- Scala class ComparisonSite and objects Greater, Greq, Leq, Less, and Inequal
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

package orc.lib.comp

import orc.types.{ BooleanType, SimpleFunctionType, Top }
import orc.values.sites.{ FunctionalSite, OverloadedDirectInvokerMethod2, TalkativeSite, IllegalArgumentInvoker }

abstract class ComparisonSite extends OverloadedDirectInvokerMethod2[AnyRef, AnyRef] with FunctionalSite {
  def getInvokerSpecialized(arg1: AnyRef, arg2: AnyRef) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: java.lang.Double, b: java.lang.Double) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: java.math.BigDecimal, b: java.math.BigDecimal) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: java.lang.Long, b: java.lang.Long) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: java.math.BigInteger, b: java.math.BigInteger) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b)))
      case (a: BigDecimal, b: Number) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b.doubleValue())))
      case (a: Number, b: BigDecimal) =>
        invoker(a, b)((a, b) => compare(-b.compareTo(a.doubleValue())))
      case (a: BigInt, b: Number) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b.longValue())))
      case (a: Number, b: BigInt) =>
        invoker(a, b)((a, b) => compare(-b.compareTo(a.longValue())))
      case (a: java.lang.Double, b: Number) =>
        invoker(a, b)((a, b) => compare(a.compareTo(b.doubleValue())))
      case (a: Number, b: java.lang.Double) =>
        invoker(a, b)((a, b) => compare(a.doubleValue().compareTo(b)))
      case (a: Comparable[_], b: Comparable[_]) =>
        invoker(a, b)((a, b) =>
          try {
            compare(a.asInstanceOf[Comparable[AnyRef]].compareTo(b))
          } catch {
            case e: ClassCastException =>
              throw new IllegalArgumentException(s"$this($a, $b)", e)
          }
        )
      case (a: AnyRef, b: AnyRef) =>
        IllegalArgumentInvoker(this, Array(a, b))
    }
  }

  protected def compare(comp: Int): Boolean

  def orcType() = SimpleFunctionType(Top, Top, BooleanType)
}

case object Greater extends ComparisonSite with LocalSingletonSite {
  override def getInvokerSpecialized(arg1: AnyRef, arg2: AnyRef) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => a > b)
      case (a: java.lang.Double, b: java.lang.Double) =>
        invoker(a, b)((a, b) => a > b)
      case (a: java.lang.Long, b: java.lang.Long) =>
        invoker(a, b)((a, b) => a > b)
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invoker(a, b)((a, b) => a > b)
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)((a, b) => a > b)
      case _ =>
        super.getInvokerSpecialized(arg1, arg2)
    }
  }

  protected def compare(comp: Int): Boolean = comp > 0
  override def toString = "Greater"
}

case object Greq extends ComparisonSite with LocalSingletonSite {
  override def getInvokerSpecialized(arg1: AnyRef, arg2: AnyRef) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => a >= b)
      case (a: java.lang.Double, b: java.lang.Double) =>
        invoker(a, b)((a, b) => a >= b)
      case (a: java.lang.Long, b: java.lang.Long) =>
        invoker(a, b)((a, b) => a >= b)
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invoker(a, b)((a, b) => a >= b)
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)((a, b) => a >= b)
      case _ =>
        super.getInvokerSpecialized(arg1, arg2)
    }
  }

  protected def compare(comp: Int): Boolean = comp >= 0
  override def toString = "Greq"
}

case object Leq extends ComparisonSite with LocalSingletonSite {
  override def getInvokerSpecialized(arg1: AnyRef, arg2: AnyRef) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => a <= b)
      case (a: java.lang.Double, b: java.lang.Double) =>
        invoker(a, b)((a, b) => a <= b)
      case (a: java.lang.Long, b: java.lang.Long) =>
        invoker(a, b)((a, b) => a <= b)
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invoker(a, b)((a, b) => a <= b)
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)((a, b) => a <= b)
      case _ =>
        super.getInvokerSpecialized(arg1, arg2)
    }
  }

  protected def compare(comp: Int): Boolean = comp <= 0
  override def toString = "Leq"
}

case object Less extends ComparisonSite with LocalSingletonSite {
  override def getInvokerSpecialized(arg1: AnyRef, arg2: AnyRef) = {
    // TODO: This does not handle all possible reasonable cases and some of the priorities are weird. When we improve the numeric stack we should fix this.
    (arg1, arg2) match {
      case (a: BigDecimal, b: BigDecimal) =>
        invoker(a, b)((a, b) => a < b)
      case (a: java.lang.Double, b: java.lang.Double) =>
        invoker(a, b)((a, b) => a < b)
      case (a: java.lang.Long, b: java.lang.Long) =>
        invoker(a, b)((a, b) => a < b)
      case (a: java.lang.Integer, b: java.lang.Integer) =>
        invoker(a, b)((a, b) => a < b)
      case (a: BigInt, b: BigInt) =>
        invoker(a, b)((a, b) => a < b)
      case _ =>
        super.getInvokerSpecialized(arg1, arg2)
    }
  }

  protected def compare(comp: Int): Boolean = comp < 0
  override def toString = "Less"
}

case object Inequal extends OverloadedDirectInvokerMethod2[Any, Any] with FunctionalSite with TalkativeSite with LocalSingletonSite {
  override def name = "Inequal"

  def getInvokerSpecialized(a: Any, b: Any) = {
    invokerStaticType(a, b)((a, b) => {
      if (a == null)
        b != null
      else
        a != b
    })
  }

  def orcType() = SimpleFunctionType(Top, Top, BooleanType)
}

