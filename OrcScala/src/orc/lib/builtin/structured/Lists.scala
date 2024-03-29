//
// Lists.scala -- Implementations of list manipulation sites
// Project OrcScala
//
// Created by dkitchin on March 31, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin.structured

import orc.types.{ Bot, Covariant, FunctionType, SignalType, SimpleFunctionType, SimpleTypeConstructor, Top, TupleType, TypeVariable }
import orc.values.{ OrcTuple, Signal }
import orc.values.sites.{ FunctionalSite, LocalSingletonSite, PartialSite1Simple, SiteMetadata, StructurePairSite, TotalSite0Simple, TotalSite2Simple, TypedSite }

object ListType extends SimpleTypeConstructor("List", Covariant)

@SerialVersionUID(-3182160592903163294L)
object NilSite extends StructurePairSite(NilConstructor, NilExtractor) with Serializable with LocalSingletonSite

@SerialVersionUID(2703598179440668393L)
object NilConstructor extends TotalSite0Simple with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Nil"
  def eval() = Nil
  def orcType() = SimpleFunctionType(ListType(Bot))
}

@SerialVersionUID(3585143907461788057L)
object NilExtractor extends PartialSite1Simple[AnyRef] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Nil.unapply"
  def eval(arg: AnyRef) = {
    arg match {
      case Nil => Some(Signal)
      case _ => None
    }
  }
  def orcType() = SimpleFunctionType(ListType(Top), SignalType)
}

@SerialVersionUID(8311877438024877786L)
object ConsSite extends StructurePairSite(ConsConstructor, ConsExtractor) with Serializable with LocalSingletonSite

@SerialVersionUID(3494959098141772508L)
object ConsConstructor extends TotalSite2Simple[AnyRef, List[AnyRef]] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Cons"
  def eval(h: AnyRef, t: List[AnyRef]) = {
    h :: t
  }
  def orcType() = {
    val X = new TypeVariable()
    FunctionType(List(X), List(X, ListType(X)), ListType(X))
  }
}

@SerialVersionUID(-6338949926050200496L)
object ConsExtractor extends PartialSite1Simple[AnyRef] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Cons.unapply"
  def eval(arg: AnyRef) =
    arg match {
      case (v: AnyRef) :: vs => Some(OrcTuple(Array(v, vs)))
      case _ => None
    }

  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(ListType(X)), TupleType(List(X, ListType(X))))
  }

  override def publicationMetadata(args: List[Option[AnyRef]]): Option[SiteMetadata] = Some(OrcTuple(Array(null, null)))
}
