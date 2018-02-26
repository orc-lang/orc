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

import orc.Invoker
import orc.error.runtime.HaltException
import orc.types.{ BooleanType, FunctionType, SignalType, SimpleCallableType, SimpleFunctionType, StrictCallableType, Top, TupleType, Type }
import orc.util.ArrayExtensions.{ Array0, Array1 }
import orc.util.TypeListEnrichment.enrichTypeList
import orc.values.{ OrcTuple, Signal }
import orc.values.sites.{ FunctionalSite, OverloadedDirectInvokerMethod1, OverloadedDirectInvokerMethod2, TalkativeSite, TotalSite, TypedSite }

case object Ift extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite {
  override def name = "Ift"
  
  def getInvokerSpecialized(a: java.lang.Boolean): Invoker = {
    invoker(a)(a =>
      if (a)
        Signal
      else
        throw HaltException.SINGLETON)
  }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

case object Iff extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite {
  override def name = "Iff"
  
  def getInvokerSpecialized(a: java.lang.Boolean): Invoker = {
    invoker(a)(a =>
      if (!a)
        Signal
      else
        throw HaltException.SINGLETON)
  }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

case object Eq extends OverloadedDirectInvokerMethod2[Any, Any] with FunctionalSite with TalkativeSite {
  override def name = "Eq"
  
  def getInvokerSpecialized(a: Any, b: Any): Invoker = {
    invokerStaticType(a, b)((a, b) => {
      if (a == null) 
        b == null
      else 
        a == b
    })
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
