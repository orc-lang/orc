//
// TupleType.scala -- Scala class TupleType
// Project OrcScala
//
// $Id: TupleType.scala 2773 2011-04-20 01:12:36Z jthywissen $
//
// Created by dkitchin on Nov 20, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.ArgumentTypecheckingException

/**
 * 
 * A tuple type.
 *
 * @author dkitchin
 */
case class TupleType(elements: List[Type]) extends UnaryCallableType with StrictType {
 
   assert(elements.size > 1)
   
   override def toString = elements.mkString("(", ", ", ")")
  
   override def join(that: Type): Type = {
     that match {
       case TupleType(otherElements) => TupleType(elements join otherElements)
       case _ => super.join(that)
     }
   }
  
  override def meet(that: Type): Type = {
    that match {
      case TupleType(otherElements) => TupleType(elements meet otherElements)
      case _ => super.meet(that)
    }
  }
  
  override def <(that: Type): Boolean = {
    that match {
      case TupleType(otherElements) => elements < otherElements
      case _ => super.<(that)
    }
   }
  
  override def subst(sigma: Map[TypeVariable, Type]): Type = {
    TupleType(elements map { _ subst sigma })
  }
  
  override def call(argType: Type) = {
    argType match {
      case IntegerConstantType(i) => {
        val index = i.toInt
        if (index >= 0 && index < elements.size) {
          elements(i.toInt)
        }
        else {
          Bot
        }
      }
      case IntegerType => elements reduceLeft { _ join _ }
      case t => throw new ArgumentTypecheckingException(0, IntegerType, t)
    }
  }
  
}
