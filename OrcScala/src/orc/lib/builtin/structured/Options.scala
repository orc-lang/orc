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

import orc.types.{ Bot, Covariant, FunctionType, SignalType, SimpleFunctionType, SimpleTypeConstructor, Top, TypeVariable }
import orc.values.Signal
import orc.values.sites.{ FunctionalSite, TypedSite }
import orc.values.sites.compatibility.{ PartialSite1, StructurePairSite, TotalSite0, TotalSite1 }

object OptionType extends SimpleTypeConstructor("Option", Covariant)

@SerialVersionUID(111672818275898614L)
object NoneSite extends StructurePairSite(NoneConstructor, NoneExtractor) with Serializable

@SerialVersionUID(-427078783776597086L)
object NoneConstructor extends TotalSite0 with TypedSite with FunctionalSite with Serializable {
  override def name = "None"
  def eval() = None
  def orcType() = SimpleFunctionType(OptionType(Bot))
}

@SerialVersionUID(1097292286490160503L)
object NoneExtractor extends PartialSite1 with TypedSite with FunctionalSite with Serializable {
  override def name = "None.unapply"
  def eval(a: AnyRef) = {
    a match {
      case None => Some(Signal)
      case Some(_) => None
      case _ => None
    }
  }
  def orcType() = SimpleFunctionType(OptionType(Top), SignalType)
}

@SerialVersionUID(4417930309108966987L)
object SomeSite extends StructurePairSite(SomeConstructor, SomeExtractor) with Serializable

@SerialVersionUID(3009000043854264802L)
object SomeConstructor extends TotalSite1 with TypedSite with FunctionalSite with Serializable {
  override def name = "Some"
  def eval(a: AnyRef) = Some(a)
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(X), OptionType(X))
  }
}

@SerialVersionUID(-8183246747813035072L)
object SomeExtractor extends PartialSite1 with TypedSite with FunctionalSite with Serializable {
  override def name = "Some.unapply"
  def eval(arg: AnyRef) = {
    arg match {
      case Some(v: AnyRef) => Some(v)
      case None => None
      case _ => None
    }
  }
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(OptionType(X)), X)
  }
}
