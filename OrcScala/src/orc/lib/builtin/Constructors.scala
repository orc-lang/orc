//
// Constructors.scala -- Scala objects ___Contructor, OptionType, and ListType
// Project OrcScala
//
// $Id: Constructors.scala 2581 2011-03-21 07:41:38Z dkitchin $
//
// Created by dkitchin on June 24, 2010.
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
import orc.types.SimpleCallableType
import orc.types._


object OptionType extends SimpleTypeConstructor("Option", Covariant)
object ListType extends SimpleTypeConstructor("List", Covariant)

object TupleConstructor extends TotalSite with TypedSite {
  override def name = "Tuple"
  def evaluate(args: List[AnyRef]) = OrcTuple(args)
  
  def orcType() = new SimpleCallableType {
    def call(argTypes: List[Type]) = { TupleType(argTypes) }
  }
}

object NoneConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "None"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List() => None
      case _ => throw new ArityMismatchException(0, args.size)
  }
  val extractor = NoneExtractor
  
  def orcType() = SimpleFunctionType(OptionType(Bot))
}

object SomeConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Some"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(v) => Some(v)
      case _ => throw new ArityMismatchException(1, args.size)
  }
  val extractor = SomeExtractor
  
  def orcType() = new UnaryCallableType { def call(t: Type) = OptionType(t) }
}



object NilConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Nil"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List() => Nil
      case _ => throw new ArityMismatchException(0, args.size)
  }
  val extractor = NilExtractor
  
  def orcType() = SimpleFunctionType(ListType(Bot))
}

object ConsConstructor extends TotalSite with Extractable with TypedSite {
  override def name = "Cons"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(v, vs : List[_]) => v :: vs
      case List(_, vs) => throw new ArgumentTypeMismatchException(1, "List", if (vs != null) vs.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(2, args.size)
  }
  val extractor = ConsExtractor
  
  def orcType() = {
    val X = new TypeVariable()
    FunctionType(List(X), List(X, ListType(X)), ListType(X))
  }
}

/* 
 * Input to a RecordConstructor is a sequence of tuples, each tuple
 * being a (field,site) mapping. Eg: ((."x",Site(x)), (."y", Site(y)), (."z", Site(z))..))
 * 
 * Note that even though a Record pattern exists, the RecordConstructor
 * is not Extractable, since record extraction is a specific two-step process,
 * parametrized by the target shape of the record.
 */
object RecordConstructor extends TotalSite with TypedSite {
  override def name = "Record"
  override def evaluate(args: List[AnyRef]) = {
    val valueMap = new scala.collection.mutable.HashMap[String,AnyRef]()
    args.zipWithIndex map
      { case (v: AnyRef, i: Int) =>
          v match {
            case OrcTuple(List(Field(key), value : AnyRef)) =>
              valueMap += ( (key,value) )
            case _ => throw new ArgumentTypeMismatchException(i, "(Field, _)", if (v != null) v.getClass().getCanonicalName() else "null")
          }
      }
    OrcRecord(scala.collection.immutable.HashMap.empty ++ valueMap)
  }
  
  def orcType() = new SimpleCallableType {
    def call(argTypes: List[Type]) = { 
      val bindings = 
        (argTypes.zipWithIndex) map {
          case (TupleType(List(FieldType(f), t)), _) => (f, t)
          case (t, i) => throw new ArgumentTypeMismatchException(i, "(Field, _)", t.toString)
        }
      RecordType(bindings.toMap)
    }
  }
}
