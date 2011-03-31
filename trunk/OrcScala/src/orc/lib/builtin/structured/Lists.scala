//
// Lists.scala -- Implementations of list manipulation sites
// Project OrcScala
//
// $Id: Constructors.scala 2637 2011-03-28 20:27:25Z dkitchin $
//
// Created by dkitchin on March 31, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.builtin.structured

import orc.error.runtime.ArgumentTypeMismatchException
import orc.values.{OrcRecord, OrcTuple, Signal}
import orc.values.sites._
import orc.types._


object ListType extends SimpleTypeConstructor("List", Covariant)

object NilSite extends StructurePairSite(NilConstructor, NilExtractor)
object NilConstructor extends TotalSite0 with TypedSite {
  override def name = "Nil"
  def eval() = Nil
  def orcType() = SimpleFunctionType(ListType(Bot))
}
object NilExtractor extends PartialSite1 with TypedSite {
  override def name = "Nil.unapply"
  def eval(arg: AnyRef) = {
    arg match {
      case Nil => Some(Signal)
      case _::_ => None
      case arr: Array[AnyRef] => {
        if (arr.size == 0) { Some(Signal) } else { None }
      }
      case c: java.util.List[_] => {
        if (c.size == 0) { Some(Signal) } else { None }
      }
      case _ => None
    }
  }
  def orcType() = SimpleFunctionType(ListType(Top), SignalType)
}


object ConsSite extends StructurePairSite(ConsConstructor, ConsExtractor)
object ConsConstructor extends TotalSite2 with TypedSite {
  override def name = "Cons"
  def eval(h: AnyRef, t: AnyRef) = {
    t match {
      case tl : List[_] => h :: tl
      case _ => throw new ArgumentTypeMismatchException(1, "List", if (t != null) t.getClass().toString() else "null")
    }
  }
  def orcType() = {
    val X = new TypeVariable()
    FunctionType(List(X), List(X, ListType(X)), ListType(X))
  }
}
object ConsExtractor extends PartialSite1 with TypedSite {
  override def name = "Cons.unapply"
  def eval(arg: AnyRef) =
    arg match {
      case (v : AnyRef) :: vs => Some(OrcTuple(List(v, vs)))
      case Nil => None
      case c:java.util.List[_] => { // Allow pattern matching on Java lists too.
        if (c.size != 0) {
          Some(OrcTuple(List(c.get(0).asInstanceOf[AnyRef], c.subList(1, c.size))))
        }
        else None
      }
      case arr: Array[AnyRef] => { // Allow List-like pattern matching on arrays.
        if (arr.size != 0) { 
          Some(OrcTuple(List(arr(0), arr.slice(1, arr.size))))
        }
        else None
      }
      case _ => None
  }
  
  def orcType() = {
    val X = new TypeVariable()
    new FunctionType(List(X), List(ListType(X)), TupleType(List(X, ListType(X))))
  }
  
}

