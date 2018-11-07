//
// Fundamental.scala -- Scala objects Ift, Iff, Eq, and Let
// Project OrcScala
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin

import orc.OrcRuntime
import orc.error.runtime.HaltException
import orc.types.{ BooleanType, FunctionType, SignalType, SimpleCallableType, SimpleFunctionType, StrictCallableType, Top, TupleType, Type }
import orc.util.ArrayExtensions.{ Array0, Array1 }
import orc.util.TypeListEnrichment.enrichTypeList
import orc.values.{ OrcTuple, Signal }
import orc.values.sites.{ FunctionalSite, LocalSingletonSite, OverloadedDirectInvokerMethod1, OverloadedDirectInvokerMethod2, TalkativeSite, TotalSiteBase, TypedSite }

@SerialVersionUID(1713576028304864566L)
case object Ift extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Ift"

  def getInvokerSpecialized(a: java.lang.Boolean) = {
    invokerInline(a)(a =>
      if (a)
        Signal
      else
        throw new HaltException)
  }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

@SerialVersionUID(7595428578485445916L)
case object Iff extends OverloadedDirectInvokerMethod1[java.lang.Boolean] with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Iff"

  def getInvokerSpecialized(a: java.lang.Boolean) = {
    invokerInline(a)(a =>
      if (!a)
        Signal
      else
        throw new HaltException)
  }

  def orcType() = SimpleFunctionType(BooleanType, SignalType)
}

@SerialVersionUID(7152101636414367959L)
case object Eq extends OverloadedDirectInvokerMethod2[Any, Any] with FunctionalSite with TalkativeSite with Serializable with LocalSingletonSite {
  override def name = "Eq"

  def getInvokerSpecialized(a: Any, b: Any) = {
    invokerStaticType(a, b)((a, b) => {
      if (a == null)
        b == null
      else
        a == b
    })
  }

  def orcType() = SimpleFunctionType(Top, Top, BooleanType)
}

@SerialVersionUID(5555898947968354991L)
object Let extends TotalSiteBase with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "let"
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) =
    args match {
      case Array0() => invokerInline(this)((_, _) => Signal)
      case Array1(v) => invoker(this)((_, vs) => vs(0))
      case vs => invoker(this)((_, vs) => OrcTuple(vs))
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
