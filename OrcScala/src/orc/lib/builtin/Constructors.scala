//
// Constructors.scala -- Scala objects ___Contructor, OptionType, and ListType
// Project OrcScala
//
// $Id$
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
import orc.error.compiletime.typing.ArgumentTypecheckingException
import orc.error.compiletime.typing.ExpectedType
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
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

object NoneConstructor extends TotalSite0 with Extractable with TypedSite {
  override def name = "None"
    
  def eval() = None
  
  val extractor = NoneExtractor
  
  def orcType() = SimpleFunctionType(OptionType(Bot))
}

object SomeConstructor extends TotalSite1 with Extractable with TypedSite {
  override def name = "Some"
    
  def eval(a: AnyRef) = Some(a)
  
  val extractor = SomeExtractor
  
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(X), OptionType(X))
  }
}



object NilConstructor extends TotalSite0 with Extractable with TypedSite {
  override def name = "Nil"
  
  def eval() = Nil
  
  val extractor = NilExtractor
  
  def orcType() = SimpleFunctionType(ListType(Bot))
}

object ConsConstructor extends TotalSite2 with Extractable with TypedSite {
  override def name = "Cons"
  def eval(h: AnyRef, t: AnyRef) = {
    t match {
      case tl : List[_] => h :: tl
      case _ => throw new ArgumentTypeMismatchException(1, "List", if (t != null) t.getClass().toString() else "null")
    }
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
          case (t, i) => throw new ArgumentTypecheckingException(i, TupleType(List(ExpectedType("of some field"), Top)), t)
        }
      RecordType(bindings.toMap)
    }
  }
}
