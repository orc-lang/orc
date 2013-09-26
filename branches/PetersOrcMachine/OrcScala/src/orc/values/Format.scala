//
// Format.scala -- Scala object Format
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

/** Format values (given as Scala's type AnyRef)
  *
  * A value which can be written in an Orc program is formatted as a string which
  * the parser would parse as an expression evaluating to that value.
  *
  * A value which cannot be written in an Orc program is given back in some
  * suitable pseudo-syntax.
  *
  * @author dkitchin
  */
object Format {

  def formatValue(v: Any): String = {
    // Escape strings by default
    formatValue(v, true)
  }

  def formatValue(v: Any, escapeStrings: Boolean): String =
    v match {
      case null => "null"
      case l: List[_] => "[" + formatSequence(l) + "]"
      case s: String => if (escapeStrings) { unparseString(s) } else s
      case Some(v) => "Some(" + formatValue(v) + ")"
      case None => "None()"
      case orcv: OrcValue => orcv.toOrcSyntax()
      case other => other.toString()
    }

  // For Java callers:
  def formatValueR(v: AnyRef): String = formatValue(v)
  def formatValueR(v: AnyRef, escapeStrings: Boolean): String = formatValue(v, escapeStrings)

  def formatSequence(vs: List[_]) =
    vs match {
      case Nil => ""
      case _ => (vs map { formatValue }) reduceRight { _ + ", " + _ }
    }

  def unparseString(s: String) = {
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
  }

}
