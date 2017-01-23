//
// Fundamental.scala -- Scala objects Ift, Iff, Eq, and Let
// Project OrcScala
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException }
import orc.types._
import orc.util.TypeListEnrichment._
import orc.util.ArrayExtensions.{ Array1, Array2, Array0 }
import orc.values._
import orc.values.sites.{ FunctionalSite, PartialSite, TotalSite, TypedSite }

object Ift extends PartialSite with TypedSite with FunctionalSite {
  override def name = "Ift"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array1(b: java.lang.Boolean) =>
        if (b.booleanValue) { Some(Signal) } else { None }
      case Array1(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

object Iff extends PartialSite with TypedSite with FunctionalSite {
  override def name = "Iff"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array1(b: java.lang.Boolean) =>
        if (b.booleanValue) { None } else { Some(Signal) }
      case Array1(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

object Eq extends TotalSite with TypedSite with FunctionalSite {
  override def name = "Eq"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array2(null, b) => new java.lang.Boolean(b == null)
      case Array2(a, b) => new java.lang.Boolean(a == b)
      case _ => throw new ArityMismatchException(2, args.size)
    }

  def orcType() = SimpleFunctionType(Top, Top, BooleanType)
}

object Let extends TotalSite with TypedSite with FunctionalSite {
  override def name = "let"
  def evaluate(args: Array[AnyRef]) =
    args match {
      case Array0() => Signal
      case Array1(v) => v
      case vs => OrcTuple(vs)
    }

  def orcType() = LetType
}

object LetType extends SimpleCallableType with StrictCallableType {

  def call(argTypes: List[Type]): Type = {
    argTypes match {
      case List() => SignalType
      case List(t) => t
      case ts => TupleType(ts)
    }
  }

  override def <(that: Type): Boolean = {
    that match {
      case FunctionType(Nil, Nil, `SignalType`) => true
      case FunctionType(_, List(t), u) if (t < u) => true
      case FunctionType(_, ts, TupleType(us)) if (ts < us) => true
      case _ => super.<(that)
    }
  }

}
