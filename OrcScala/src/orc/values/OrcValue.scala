//
// Value.scala -- Scala class Value
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
  def letLike(vs: Seq[AnyRef]) = {
    vs match {
      case Seq() => Signal
      case Seq(v) => v
      case _ => OrcTuple(vs.toArray)
    }
  }

}

// Some very simple values

case object Signal extends OrcValue {
  override def toOrcSyntax() = "signal"
}

class Field private (_name: String) extends OrcValue with Serializable {
  val name = _name.intern()

  override def toOrcSyntax() = "." + name

  @deprecated("Use name instead.", "3.0")
  def field = name

  override def equals(o: Any): Boolean = o match {
    case Field(oname) => name eq oname
    case _ => false
  }

  override def hashCode(): Int = name.hashCode()
}

object Field {
  def apply(name: String) = new Field(name)
  def unapply(f: Field): Option[String] = Some(f.name)

  /** Create a Field object.
    *
    * This exists for Java since apply is not wrapped in a static method.
    */
  def create(name: String) = apply(name)
}

class Tag(val name: String)

case class TaggedValue(tag: Tag, values: Array[AnyRef]) extends OrcValue {
  override def toOrcSyntax = tag.name + "(" + Format.formatSequence(values) + ")"
}
