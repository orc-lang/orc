//
// Accessors.scala -- Scala class/trait/object Accessors
// Project OrcScala
//
// $Id$
//
// Created by amp on Jul 4, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.values.sites._
import orc.values.OrcRecord
import orc.values.Field
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.NoSuchMemberException
import orc.types.UnaryCallableType
import orc.types.BinaryCallableType
import orc.values.OrcTuple
import orc.types.TupleType
import orc.error.compiletime.typing.ArgumentTypecheckingException
import orc.types.Type
import orc.error.compiletime.typing.ExpectedType
import orc.values.OrcValue
import orc.values.OrcRecord

/**
  *
  * @author amp
  */
object GetField extends PartialSite2 {
  override def name = "GetField"
  def eval(v: AnyRef, f: AnyRef) = {
    val field = f match {
      case f: Field => f
      case _ => throw new ArgumentTypeMismatchException(1, "Field", f.getClass().getCanonicalName())
    }
    v match {
      case v: HasFields => v.getField(field)
      case _: OrcValue => 
        throw new ArgumentTypeMismatchException(0, "value with field support", v.getClass().getCanonicalName())
      case _ => // Otherwise we are some java thing so call into Java handlers
        Some(new JavaMemberProxy(v, field.field))
        // TODO: Concider lifting this check into Porc.
    }
  }

  /*TODO: There needs to be a type here. I think this will require a HasFields type.
  def orcType() = new BinaryCallableType {
    def call(vT: Type, fT: Type): Type = {
      (vT, fT) match {
        case (_, fT) if  => throw new ArgumentTypecheckingException(0, ExpectedType("a function type"), g)
        case f: FunctionType => f
      }
    }
  }
  */

  override val effectFree = true
}

object GetElem extends PartialSite2 {
  override def name = "GetElem"
  def eval(v: AnyRef, i: AnyRef) = {
    val index = i match {
      case v: Number => v.intValue
      case _ => throw new ArgumentTypeMismatchException(1, "number", i.getClass().getCanonicalName())
    }
    v match {
      case v: OrcTuple => v.getElem(index)
      case _ => throw new ArgumentTypeMismatchException(0, "value with element support", v.getClass().getCanonicalName())
    }
  }

  def orcType() = new BinaryCallableType {
    def call(vT: Type, fT: Type): Type = {
      (vT, fT) match {
        case (tT: TupleType, _) => tT.call(fT)
        case _ => throw new ArgumentTypecheckingException(0, ExpectedType("a tuple type"), vT)
      }
    }
  }

  override val effectFree = true
}

object ProjectClosure extends TotalSite1 {
  override def name = "ProjectClosure"
  def eval(v: AnyRef) = {
    v match {
      case v: OrcRecord if v.entries.contains("apply") => 
        eval(v.entries("apply"))
      case _ => v // TODO: Should this check if it is a real callable?
    }
  }
 
  /*
  def orcType() = new BinaryCallableType {
    def call(vT: Type, fT: Type): Type = {
      (vT, fT) match {
        case (tT: TupleType, _) => tT.call(fT)
        case _ => throw new ArgumentTypecheckingException(0, ExpectedType("a tuple type"), vT)
      }
    }
  }
  */

  override val effectFree = true
  override val immediatePublish = true
  override val publications = (1, Some(1))
}


object ProjectUnapply extends TotalSite1 {
  override def name = "ProjectUnapply"
  def eval(v: AnyRef) = {
    v match {
      case v: OrcRecord if v.entries.contains("unapply") => 
        eval(v.entries("unapply"))
      case _: HasFields => eval(GetField.eval(v, Field("unapply")))
      case _ => v
    }
  }
 
  /*
  def orcType() = new BinaryCallableType {
    def call(vT: Type, fT: Type): Type = {
      (vT, fT) match {
        case (tT: TupleType, _) => tT.call(fT)
        case _ => throw new ArgumentTypecheckingException(0, ExpectedType("a tuple type"), vT)
      }
    }
  }
  */

  override val effectFree = true
}