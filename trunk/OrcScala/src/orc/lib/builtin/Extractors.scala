//
// Extractors.scala -- Scala objects ___Extractor
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.compiletime.typing._
import orc.util.OptionMapExtension._
import orc.types._
import orc.Handle

object NoneExtractor extends PartialSite with TypedSite {
  override def name = "None?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(None) => Some(Signal)
      case List(Some(_)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
  }
  
  def orcType() = SimpleFunctionType(OptionType(Top), SignalType)
}


object SomeExtractor extends PartialSite with TypedSite {
  override def name = "Some?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(Some(v : AnyRef)) => Some(v)
      case List(None) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Option", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
  }
  
  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        case OptionType(t) => t
        case t => throw new ArgumentTypeMismatchException(0, "Option[_]", t.toString)
      }
    }
  }
}


object NilExtractor extends PartialSite with TypedSite {
  override def name = "Nil?"
  def evaluate(args: List[AnyRef]) = {
    args match {
      case List(Nil) => Some(Signal)
      case List(_::_) => None
      case List(arr: Array[AnyRef]) if (arr.size == 0) => Some(Signal)
      case List(arr: Array[AnyRef]) if (arr.size != 0) => None
      case List(c:java.util.List[_]) if (c.size == 0) => Some(Signal)
      case List(c:java.util.List[_]) if (c.size != 0) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
  def orcType() = SimpleFunctionType(ListType(Top), SignalType)
}


object ConsExtractor extends PartialSite with TypedSite {
  override def name = "Cons?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List((v : AnyRef) :: vs) => Some(OrcTuple(List(v, vs)))
      case List(Nil) => None
      case List(c:java.util.List[_]) if (c.size != 0) => {
        Some(OrcTuple(List(c.get(0).asInstanceOf[AnyRef], c.subList(1, c.size))))
      }
      case List(c:java.util.List[_]) if (c.size == 0) => None
      case List(arr: Array[AnyRef]) if (arr.size != 0) => { // Allow List-like pattern matching on arrays.
        Some(OrcTuple(List(arr(0), arr.slice(1, arr.size))))
      }
      case List(arr: Array[AnyRef]) if (arr.size == 0) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
  }
  
  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        case ListType(t) => TupleType(List(t, ListType(t)))
        case t => throw new ArgumentTypeMismatchException(0, "List[_]", t.toString)
      }
    }
  }
}


/* 
 * Checks if a Tuple t has a given number of elements.
 * If the check succeeds, the Some(t) is returned, 
 * else None.
 */
object TupleArityChecker extends PartialSite with TypedSite {
  override def name = "TupleArityChecker?"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(OrcTuple(elems), arity: BigInt) =>
        if (elems.size == arity) {
          Some(OrcTuple(elems))
        } else {
          None
        }
      case List(_ : OrcTuple, a) => throw new ArgumentTypeMismatchException(1, "Integer", if (a != null) a.getClass().toString() else "null")
      case List(a, _) => throw new ArgumentTypeMismatchException(0, "Tuple", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(2, args.size)
  }
  
  def orcType() = new SimpleCallableType {
    def call(argTypes: List[Type]): Type = {
      argTypes match {
        case List(t@ TupleType(elements), IntegerConstantType(i)) => {
          if (elements.size != i) {
            throw new TupleSizeException(i.toInt, elements.size)
          }
          OptionType(t)
        }
        case List(t : TupleType, IntegerType) => {
          OptionType(t)
        }
        case List(_ : TupleType, t) => throw new ArgumentTypeMismatchException(1, "Integer", t.toString)
        case List(t, _) => throw new ArgumentTypeMismatchException(0, "Tuple", t.toString)
        case _ => throw new ArityMismatchException(2, argTypes.size)
      }
    }
  }
  
}


object RecordMatcher extends PartialSite with TypedSite {
  override def name = "Record?"
    
  override def evaluate(args: List[AnyRef]): Option[AnyRef] =
    args match {  
      case List(OrcRecord(entries), shape@ _*) => {
        val matchedValues: Option[List[AnyRef]] = 
          shape.toList.zipWithIndex optionMap { 
            case (Field(f),_) => entries get f
            case (a,i) => throw new ArgumentTypeMismatchException(i+1, "Field", if (a != null) a.getClass().toString() else "null")
          } 
        matchedValues map { OrcValue.letLike }
      }
      case List(_, _*) => None
    } 
  
  
  def orcType() = new SimpleCallableType {
    def call(argTypes: List[Type]): Type = {
      argTypes match {
        case List(rt @ RecordType(entries), shape @ _*) => {
          val matchedElements = 
            shape.toList.zipWithIndex map {
              case (FieldType(f),_) => entries.getOrElse(f, throw new RecordShapeMismatchException(rt, f)) 
              case (t,i) => throw new ArgumentTypeMismatchException(i+1, "Record", t.toString)
            }
          letLike(matchedElements)
        }
        case List(t,_*) => throw new ArgumentTypeMismatchException(0, "Record", t.toString)
      }
    }
  }
}
