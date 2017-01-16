//
// Accessors.scala -- Scala class/trait/object Accessors
// Project OrcScala
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

/*
import orc.error.compiletime.typing.{ArgumentTypecheckingException, ExpectedType}
import orc.error.runtime.{ArgumentTypeMismatchException, NoSuchMemberException}
import orc.types.{BinaryCallableType, FieldType, HasFieldsType, RecordType, TupleType, Type, UnaryCallableType}
import orc.values.{Field, OrcRecord, OrcTuple, OrcValue}
import orc.values.sites._
import orc.types.HasFieldsType
import orc.types.IntegerType
import orc.types.IntegerConstantType
import orc.types.JavaObjectType
import orc.types.NumberType

object GetField extends PartialSite2 with TypedSite {
  override def name = "GetField"
  def eval(v: AnyRef, f: AnyRef) = {
    val field = f match {
      case f: Field => f
      case _ => throw new ArgumentTypeMismatchException(1, "Field", f.getClass().getCanonicalName())
    }
    v match {
      case v: HasFields => Some(v.getField(field))
      case _: OrcValue => 
        throw new ArgumentTypeMismatchException(0, "value with field support", v.getClass().getCanonicalName())
      case _ => // Otherwise we are some java thing so call into Java handlers
        Some(new JavaMemberProxy(v, field.field))
    }
  }

  def orcType() = new BinaryCallableType {
    def call(vT: Type, fT: Type): Type = {
      (vT, fT) match {
        case (hfT : HasFieldsType, f : FieldType) => hfT.getField(f)
        case (hfT : HasFieldsType, _) => 
          throw new ArgumentTypecheckingException(1, ExpectedType("field"), fT)
          
        // Special cases for numeric types allowing access to java fields
        case (`IntegerType` | IntegerConstantType(_), _) => {
          call(JavaObjectType(classOf[java.math.BigInteger]), fT)
        }
        case (`NumberType`, _) => {
          call(JavaObjectType(classOf[java.math.BigDecimal]), fT)
        }
        
        case (_, _) => 
          throw new ArgumentTypecheckingException(0, ExpectedType("with fields"), vT)
      }
    }
  }

  override val effectFree = true
}

object GetElem extends PartialSite2 with TypedSite {
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

object ProjectClosure extends TotalSite1 with TypedSite with FunctionalSite with TalkativeSite {
  override def name = "ProjectClosure"
  def eval(v: AnyRef) = {
    v match {
      case c: JavaClassProxy => {
        // Don't let a static apply shadow the constructor
        c
      }
      case f: HasFields if f.hasField(Field("apply")) => { 
        eval(f.getField(Field("apply")))
      }
      /*case v: OrcRecord if v.entries.contains("apply") => 
        eval(v.entries("apply"))*/
      case _ => v // TODO: Should this check if it is a real callable?
    }
  }
 
  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        //case t:RecordType if t.entries.contains("apply") => call(t.entries("apply"))
        case t:HasFieldsType if t.hasField(FieldType("apply")) => call(t.getField(FieldType("apply")))
        case _ => argType
      }
    }
  }
}


object ProjectUnapply extends TotalSite1 with TypedSite with FunctionalSite with TalkativeSite {
  override def name = "ProjectUnapply"
  def eval(v: AnyRef) = {
    v match {
      /*case v: OrcRecord if v.entries.contains("unapply") => 
        eval(v.entries("unapply"))*/
      case v: HasFields if v.hasField(Field("unapply")) => eval(v.getField(Field("unapply")))
      case _ => v
    }
  }
 
  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        //case t:RecordType if t.entries.contains("unapply") => call(t.entries("unapply"))
        case t:HasFieldsType if t.hasField(FieldType("unapply")) => call(t.getField(FieldType("unapply")))
        case _ => argType
      }
    }
  }
}
*/
