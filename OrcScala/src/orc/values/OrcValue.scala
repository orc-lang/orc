//
// Value.scala -- Scala class Value
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values

/** An Orc-specific value, such as: a closure, a tagged value
  * of a user-defined Orc datatype, or a signal.
  *
  * @author dkitchin
  */
trait OrcValue extends AnyRef {

  /** The method toOrcSyntax has the following contract:
    *
    * A value which can be written in an Orc program is formatted as a string which
    * the parser would parse as an expression evaluating to that value.
    *
    * A value which cannot be written in an Orc program is formatted in some
    * readable pseudo-syntax, but with no guarantees about its parsability.
    * @return
    */
  def toOrcSyntax(): String = super.toString()
  override def toString() = toOrcSyntax()

}

object OrcValue {

  /** Condense a list of values, using the classic Let site behavior.
    * @param vs
    * @return
    */
  def letLike(vs: List[AnyRef]) = {
    vs match {
      case Nil => Signal
      case List(v) => v
      case _ => OrcTuple(vs)
    }
  }

}

// Some very simple values

case object Signal extends OrcValue {
  override def toOrcSyntax() = "signal"
  override val hashCode = super.hashCode() // Only need to compute this once for an immutable object
}

case class Field(field: String) extends OrcValue {
  override def toOrcSyntax() = "." + field
}

class Tag(val name: String)

case class TaggedValue(tag: Tag, values: List[AnyRef]) extends OrcValue {
  override def toOrcSyntax = tag.name + "(" + Format.formatSequence(values) + ")"
}
