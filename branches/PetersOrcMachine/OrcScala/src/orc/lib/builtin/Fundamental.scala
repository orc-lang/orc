//
// Fundamental.scala -- Scala objects Ift, Iff, Eq, and Let
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.types._
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.util.TypeListEnrichment._

object Ift extends PartialSite with TypedSite {
  override def name = "Ift"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(b: java.lang.Boolean) =>
        if (b.booleanValue) { Some(Signal) } else { None }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)

  override val effectFree = true
}

object Iff extends PartialSite with TypedSite {
  override def name = "Iff"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(b: java.lang.Boolean) =>
        if (b.booleanValue) { None } else { Some(Signal) }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)

  override val effectFree = true
}

object Eq extends TotalSite with TypedSite {
  override def name = "Eq"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(null, b) => new java.lang.Boolean(b == null)
      case List(a, b) => new java.lang.Boolean(a == b)
      case _ => throw new ArityMismatchException(2, args.size)
    }

  def orcType() = SimpleFunctionType(Top, Top, BooleanType)

  override val effectFree = true
}

object Let extends TotalSite with TypedSite {
  override def name = "let"
  def evaluate(args: List[AnyRef]) =
    args match {
      case Nil => Signal
      case (v: AnyRef) :: Nil => v
      case (vs: List[_]) => OrcTuple(vs)
    }

  def orcType() = LetType

  override val effectFree = true
}

object LetType extends SimpleCallableType with StrictType {

  def call(argTypes: List[Type]): Type = {
    argTypes match {
      case Nil => SignalType
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
