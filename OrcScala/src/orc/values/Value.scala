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

abstract class Value() {
  def toOrcSyntax(): String = toString()
}

case class Literal(value: Any) extends Value {
  override def toOrcSyntax() = value match {
    case null => "null"
    case s: String => "\"" + s.replace("\"", "\\\"").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    case _ => value.toString()
  }
  override def toString() = toOrcSyntax()
}

case object Signal extends Value {
  override def toOrcSyntax() = "signal"
}

case class Field(field: String) extends Value
