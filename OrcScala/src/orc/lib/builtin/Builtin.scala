//
// Builtin.scala -- Collection of objects implementing Orc fundamental sites
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/**
 * @authors dkitchin, jthywiss
 */

package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values.sites._
import orc.values.Value
import orc.values.Literal
import orc.values.Signal


// Logic

object If extends PartialSite with UntypedSite {
  override def name = "If"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal({}))
      case _ => None
  }
}

object Not extends PartialSite with UntypedSite {
  override def name = "Not"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal(false))
      case List(Literal(false)) => Some(Literal(true))
      case _ => None
  }
}

object Eq extends PartialSite with UntypedSite {
  override def name = "Eq"
  def evaluate(args: List[Value]) =
    args match {
      case List(a,b) => Some(Literal(a equals b))
      case _ => None
  }
}



// Constructors

case class OrcTuple(elements: List[Value]) extends PartialSite with UntypedSite {
  def evaluate(args: List[Value]) = 
    args match {
  	  case List(Literal(i: Int)) if (0 <= i) && (i < elements.size) => Some(elements(i))
  	  case _ => None
    }
}
case class OrcList(elements: List[Value]) extends Value
case class OrcOption(contents: Option[Value]) extends Value


object TupleConstructor extends TotalSite with UntypedSite {
  override def name = "Tuple"
  def evaluate(args: List[Value]) = OrcTuple(args)
}


object NoneConstructor extends PartialSite with UntypedSite {
  override def name = "None"
  def evaluate(args: List[Value]) =
    args match {
      case List() => Some(OrcOption(None))
      case _ => None
  }
}

object SomeConstructor extends PartialSite {
  override def name = "Some"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(v) => Some(OrcOption(Some(v)))
      case _ => None
  }
}



object NilConstructor extends PartialSite with UntypedSite {
  override def name = "Nil"
  def evaluate(args: List[Value]) =
    args match {
      case List() => Some(OrcList(Nil))
      case _ => None
  }
}

object ConsConstructor extends PartialSite with UntypedSite {
  override def name = "Cons"
  def evaluate(args: List[Value]) =
    args match {
      case List(v, OrcList(vs)) => Some(OrcList(v :: vs))
      case _ => None
  }
}

object RecordConstructor extends UnimplementedSite



// Extractors

object NoneExtractor extends PartialSite with UntypedSite {
  override def name = "None?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(None)) => Some(Signal)
      case _ => None
  }
}

object SomeExtractor extends PartialSite {
  override def name = "Some?"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(Some(v))) => Some(v)
      case _ => None
  }
}



object NilExtractor extends PartialSite with UntypedSite {
  override def name = "Nil?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(Nil)) => Some(Signal)
      case _ => None
  }
}

object ConsExtractor extends PartialSite with UntypedSite {
  override def name = "Cons?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(v :: vs)) => Some(OrcTuple(List(v, OrcList(vs))))
      case _ => None
  }
}

object FindExtractor extends UnimplementedSite


// Site site

object SiteSite extends UnimplementedSite

