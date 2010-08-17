//
// Types.scala -- Scala package orc.types
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.types

import orc.error.compiletime.typing.SubtypeFailureException

abstract class Type {
  def join(that: Type): Type
  def meet(that: Type): Type
  def assertSubtype(that: Type)
}

object TypeConversions {
  implicit def type2type(that: orc.ast.oil.nameless.Type): Type = that match {
    case orc.ast.oil.nameless.Top() => Top
    case orc.ast.oil.nameless.Bot() => Bot
    case orc.ast.oil.nameless.FunctionType(a, p, r) => ArrowType(a, p map type2type, r)
    case orc.ast.oil.nameless.TypeVar(i) => TypeVar(i)
  }
  implicit def typelist2typelist(that: List[orc.ast.oil.nameless.Type]): List[Type] = that map type2type
}

case object Top extends Type {
  def join(that: Type): Type = this
  def meet(that: Type): Type = that
  def assertSubtype(that: Type) {
    that match {
      case Top => {}
      case _ => throw new SubtypeFailureException(this, that) //FIXME: Supply source location 
    }
  }
}

case object Bot extends Type {
  def join(that: Type): Type = that
  def meet(that: Type): Type = this
  def assertSubtype(that: Type) {}
}

case class ArrowType(typeFormalArity: Int, argTypes: List[Type], returnType: Type) extends Type {
  def join(that: Type): Type = that match {
    case ArrowType(thatTypeFormalArity, thatargTypes, thatReturnType) if (sameShape(that)) => {
      val combinedargTypes = for ((t1, t2) <- argTypes zip thatargTypes) yield t1 meet t2
      ArrowType(thatTypeFormalArity, combinedargTypes, returnType join thatReturnType)
    }
    case _ => Top
  }
  def meet(that: Type): Type = that match {
    case ArrowType(thatTypeFormalArity, thatargTypes, thatReturnType) if (sameShape(that)) => {
      val combinedargTypes = for ((t1, t2) <- argTypes zip thatargTypes) yield t1 join t2
      ArrowType(thatTypeFormalArity, combinedargTypes, returnType meet thatReturnType)
    }
    case _ => Bot
  }
  def assertSubtype(that: Type) {
    that match {
      case ArrowType(thatTypeFormalArity, thatargTypes, thatReturnType) if (sameShape(that)) => {
        val combinedargTypes = for ((t1, t2) <- argTypes zip thatargTypes) yield t1 meet t2
        returnType assertSubtype thatReturnType
      }
      case Top => {}
      case _ => throw new SubtypeFailureException(this, that) //FIXME: Supply source location
    }
  }
  def sameShape(that: Type): Boolean = that match {
    case ArrowType(thatTypeFormalArity, thatargTypes, thatReturnType) => (typeFormalArity == thatTypeFormalArity) && (argTypes.size == thatargTypes.size)
    case _ => false
  }
}

case class TypeVar(index: Int) extends Type {
  def join(that: Type): Type = that match {
    case TypeVar(thatIndex) if (index == thatIndex) => this
    case _ => Top
  }
  def meet(that: Type): Type = that match {
    case TypeVar(thatIndex) if (index == thatIndex) => this
    case _ => Bot
  }
  def assertSubtype(that: Type) {
    that match {
      case TypeVar(thatIndex) if (index == thatIndex) => {}
      case Top => {}
      case _ => throw new SubtypeFailureException(this, that) //FIXME: Supply source location
    }
  }
}
