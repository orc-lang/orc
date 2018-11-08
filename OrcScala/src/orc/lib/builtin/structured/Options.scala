//
// Options.scala -- Implementations of option manipulation sites
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

import orc.OrcRuntime
import orc.types._
import orc.values._
import orc.values.sites.{ FunctionalSite, LocalSingletonSite, TypedSite }
import orc.values.sites.{ TotalSite0Base, TotalSite1Base, PartialSite1Base, StructurePairSite }

object OptionType extends SimpleTypeConstructor("Option", Covariant)

@SerialVersionUID(111672818275898614L)
object NoneSite extends StructurePairSite(NoneConstructor, NoneExtractor) with Serializable with LocalSingletonSite

@SerialVersionUID(-427078783776597086L)
object NoneConstructor extends TotalSite0Base with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "None"
  def getInvoker(runtime: OrcRuntime) = invokerInline(this) { _ => None }
  def orcType() = SimpleFunctionType(OptionType(Bot))
}

@SerialVersionUID(1097292286490160503L)
object NoneExtractor extends PartialSite1Base[Option[AnyRef]] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "None.unapply"
  def getInvoker(runtime: OrcRuntime, arg: Option[AnyRef]) = invokerInline(this, arg) { (_, a) =>
    a match {
      case None => Some(Signal)
      case Some(_) => None
    }
  }
  def orcType() = SimpleFunctionType(OptionType(Top), SignalType)
}

@SerialVersionUID(4417930309108966987L)
object SomeSite extends StructurePairSite(SomeConstructor, SomeExtractor) with Serializable with LocalSingletonSite

@SerialVersionUID(3009000043854264802L)
object SomeConstructor extends TotalSite1Base[AnyRef] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Some"
  def getInvoker(runtime: OrcRuntime, arg: AnyRef) = invokerInline(this, arg) { (_, a) =>
    Some(a)
  }
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(X), OptionType(X))
  }
}

@SerialVersionUID(-8183246747813035072L)
object SomeExtractor extends PartialSite1Base[Option[AnyRef]] with TypedSite with FunctionalSite with Serializable with LocalSingletonSite {
  override def name = "Some.unapply"
  def getInvoker(runtime: OrcRuntime, arg: Option[AnyRef]) = invokerInline(this, arg) { (_, a) =>
    a match {
      case Some(v: AnyRef) => Some(v)
      case None => None
    }
  }
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(OptionType(X)), X)
  }
}
