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
import orc.oil.nameless.AddNames
import orc.oil.named.PrettyPrint
import orc.oil.named.BoundVar

trait OrcValue extends AnyRef {
  def toOrcSyntax(): String = super.toString() 
  override def toString() = toOrcSyntax()  
  
  def letLike(vs: List[AnyRef]) = {
    vs match {
      case Nil => Signal
      case List(v) => v
      case _ => OrcTuple(vs)
    }
  }
}

case object Signal extends OrcValue {
  override def toOrcSyntax() = "signal"
}

case class Field(field: String) extends OrcValue {
  override def toOrcSyntax() = "." + field
}

case class TaggedValue(tag: DataSite, values: List[AnyRef]) extends OrcValue {
  override def toOrcSyntax = tag.toOrcSyntax() + "(" + Format.formatSequence(values) + ")"
}

case class OrcTuple(values: List[AnyRef]) extends PartialSite with UntypedSite {
  def evaluate(args: List[AnyRef]) = 
    args match {
      case List(bi: BigInt) => {
        val i: Int = bi.intValue
        if (0 <= i  &&  i < values.size) 
          { Some(values(i)) }
        else
          { None }
      }
      case _ => None
    }
  override def toOrcSyntax() = "(" + Format.formatSequence(values) + ")" 
}

// Closures //
class Closure(d: Def, ds: List[Def]) extends OrcValue {
    val arity: Int = d.arity
    val body: Expression = d.body
    var context: List[AnyRef] = Nil
    
    override def toOrcSyntax() = {
      val (defs, rest) = context.splitAt(ds.size)
      val newctx = (defs map {_ => None}) ::: (rest map { Some(_) })
      val subdef = d.subst(newctx)
      val myName = new BoundVar()
      val defNames = 
        for (d <- defs) yield 
          if (d == this) { myName } else { new BoundVar() }
      val namedDef = AddNames.namelessToNamed(myName, subdef, defNames, Nil)
      val pp = new PrettyPrint()
      "lambda" +
        pp.reduce(namedDef.name) + 
          pp.paren(namedDef.formals) + 
            " = " + 
              pp.reduce(namedDef.body)
    }
}
object Closure {
    def unapply(c: Closure) = Some((c.arity, c.body, c.context))
}

// Records.
case class OrcRecord(entries: scala.collection.mutable.Map[String,AnyRef]) extends PartialSite with UntypedSite {
  override def evaluate(args: List[AnyRef]) = 
    args match {
      case List(Field(name)) => entries.get(name)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Field", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
    }
  
  override def toOrcSyntax() = {
    val formattedEntries = 
      for ( (s,v) <- entries) yield { 
        s + " = " + Format.formatValue(v) 
      }
    val contents = 
      formattedEntries.toList match {
        case Nil => ""
        case l => l reduceRight { _ + ", " + _ } 
      }
    "{. " + contents + " .}"
  }
  
  /* Create a new record, extending this record with the bindings of the other record.
   * When there is overlap, the other record's bindings override this record.
   */
  def +(other: OrcRecord): OrcRecord = {
    val empty = this.entries.empty
    OrcRecord(empty ++ this.entries ++ other.entries)
  }

  /* Aliased for easier use in Java code */
  def extendWith(other: OrcRecord): OrcRecord = this + other
  
}

// TODO: Move this functionality somewhere more appropriate
object Format {
  
  def formatValue(v: AnyRef): String =
    v match {
      case null => "null"
      case l: List[AnyRef] => "[" + formatSequence(l) + "]"
      case s: String => unparseString(s)
      case orcv: OrcValue => orcv.toOrcSyntax()
      case other => other.toString()
    }
  
  def formatSequence(vs : List[AnyRef]) = 
    vs match {
      case Nil => ""
      case _ => ( vs map { formatValue } ) reduceRight { _ + ", " + _ } 
    }
  
  def unparseString(s : String) = {
    "\"" + s.replace("\"", "\\\"").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
  }
  
}