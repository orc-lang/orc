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
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

// Logic

object IfT extends PartialSite with UntypedSite {
  override def name = "IfT"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal({}))
      case List(Literal(false)) => None
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object IfF extends PartialSite with UntypedSite {
  override def name = "IfF"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => None
      case List(Literal(false)) => Some(Literal({}))
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object Not extends PartialSite with UntypedSite {
  override def name = "Not"
  def evaluate(args: List[Value]) =
    args match {
      case List(Literal(true)) => Some(Literal(false))
      case List(Literal(false)) => Some(Literal(true))
      case List(a) => throw new ArgumentTypeMismatchException(0, "Boolean", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object Eq extends PartialSite with UntypedSite {
  override def name = "Eq"
  def evaluate(args: List[Value]) =
    args match {
      case List(a,b) => Some(Literal(a equals b))
      case _ => throw new ArityMismatchException(2, args.size)
  }
}



// Constructors

object TupleConstructor extends TotalSite with UntypedSite {
  override def name = "Tuple"
  def evaluate(args: List[Value]) = OrcTuple(args)
}


object NoneConstructor extends PartialSite with UntypedSite {
  override def name = "None"
  def evaluate(args: List[Value]) =
    args match {
      case List() => Some(OrcOption(None))
      case _ => throw new ArityMismatchException(0, args.size)
  }
}

object SomeConstructor extends PartialSite {
  override def name = "Some"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(v) => Some(OrcOption(Some(v)))
      case _ => throw new ArityMismatchException(1, args.size)
  }
}



object NilConstructor extends PartialSite with UntypedSite {
  override def name = "Nil"
  def evaluate(args: List[Value]) =
    args match {
      case List() => Some(OrcList(Nil))
      case _ => throw new ArityMismatchException(0, args.size)
  }
}

object ConsConstructor extends PartialSite with UntypedSite {
  override def name = "Cons"
  def evaluate(args: List[Value]) =
    args match {
      case List(v, OrcList(vs)) => Some(OrcList(v :: vs))
      case List(v1, v2) => throw new ArgumentTypeMismatchException(1, "List", v2.getClass().toString())
      case _ => throw new ArityMismatchException(2, args.size)
  }
}

object RecordConstructor extends UnimplementedSite //FIXME:TODO: Implement



// Extractors

object NoneExtractor extends PartialSite with UntypedSite {
  override def name = "None?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(None)) => Some(Signal)
      case List(a) => throw new ArgumentTypeMismatchException(0, "None", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object SomeExtractor extends PartialSite {
  override def name = "Some?"
  def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcOption(Some(v))) => Some(v)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Some", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}



object NilExtractor extends PartialSite with UntypedSite {
  override def name = "Nil?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(Nil)) => Some(Signal)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Nil", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object ConsExtractor extends PartialSite with UntypedSite {
  override def name = "Cons?"
  def evaluate(args: List[Value]) =
    args match {
      case List(OrcList(v :: vs)) => Some(OrcTuple(List(v, OrcList(vs))))
      case List(a) => throw new ArgumentTypeMismatchException(0, "List", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

object FindExtractor extends UnimplementedSite //FIXME:TODO: Implement


// Site site

object SiteSite extends UnimplementedSite //FIXME:TODO: Implement

