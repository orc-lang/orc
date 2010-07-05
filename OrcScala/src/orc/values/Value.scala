//
// Value.scala -- Scala class Value
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

import orc.values.sites.PartialSite
import orc.values.sites.Site

import orc.values.sites.UntypedSite
import orc.oil.nameless.Def
import orc.oil.nameless.Type
import orc.oil.nameless.Expression
import orc.lib.builtin.DataSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException

import scala.collection.mutable.Map


abstract class Value extends AnyRef {
  def toOrcSyntax(): String = super.toString()
  
  def commaSepValues(vs : List[Value]) = vs match {
    case Nil => ""
    case v::Nil => v.toOrcSyntax()
    case _ => (vs map {_.toOrcSyntax()}) reduceRight { _ + ", " + _ } 
  }
  
  override def toString() = toOrcSyntax()
  
}


case class Literal(value: Any) extends Value {
  override def toOrcSyntax() = value match {
    case null => "null"
    case s: String => "\"" + s.replace("\"", "\\\"").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    case _ => value.toString()
  }
}

case object Signal extends Value {
  override def toOrcSyntax() = "signal"
}

case class Field(field: String) extends Value {
  override def toString() = "." + field
}

case class TaggedValues(tag: DataSite, values: List[Value]) extends Value {
  override def toOrcSyntax = tag.toOrcSyntax() + "(" + commaSepValues(values) + ")"
}

case class OrcTuple(elements: List[Value]) extends PartialSite with UntypedSite {
  def evaluate(args: List[Value]) = 
    args match {
      case List(Literal(i: Int)) if (0 <= i) && (i < elements.size) => Some(elements(i))
      case _ => None
    }
  override def toOrcSyntax() = "(" + commaSepValues(elements) + ")" 
  override def toString() = toOrcSyntax() /* to replace toString overriding induced by site traits */
}

case class OrcList(elements: List[Value]) extends Value {
  override def toOrcSyntax() = "[" + commaSepValues(elements) + "]"
}
case class OrcOption(contents: Option[Value]) extends Value {
  override def toOrcSyntax() = contents match {
    case Some(v) => "Some(" + v.toOrcSyntax() + ")"
    case None => "None()"
  }
}



// Closures //
class Closure(d: Def) extends Value {
    val arity: Int = d.arity
    val body: Expression = d.body
    var context: List[Value] = Nil
}
object Closure {
    def unapply(c: Closure) = Some((c.arity, c.body, c.context))
}

// Records.
case class OrcRecord(values: Map[String,Value]) extends PartialSite with UntypedSite {
  override def evaluate(args: List[Value]) = 
    args match {
      case List(Field(name)) => values.get(name)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Field", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  
  override def toOrcSyntax() = {
    val entries = for (s <- values.keys) yield { s + " = " + values(s) }
    val contents = 
      entries match {
        case Nil => " "
        case _ => entries reduceRight { _ + ", " + _ }
      }
    "{. " + contents + " .}"
  }
}