//
// Types.scala -- Scala package orc.types
// Project OrcScala
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.types

object SignalType extends Type { override def toString = "Signal" }
object NumberType extends Type { override def toString = "Number" }

object IntegerType extends Type {
  override def toString = "Integer"

  override def <(that: Type): Boolean = {
    that match {
      case `IntegerType` => true
      case _ => NumberType < that
    }
  }
}

/* A dependent type, usually used to index into tuples. */
// TODO: Allow join
case class IntegerConstantType(i: scala.math.BigInt) extends Type {

  override def toString = IntegerType.toString

  override def join(that: Type): Type = {
    that match {
      case IntegerConstantType(j) if (i equals j) => this
      case IntegerConstantType(_) => IntegerType
      case Bot => this
      case _ => IntegerType join that
    }
  }

  override def meet(that: Type): Type = {
    that match {
      case IntegerConstantType(j) if (i equals j) => this
      case IntegerConstantType(_) => Bot
      case Top => this
      case _ => Bot
    }
  }

  override def <(that: Type): Boolean = {
    that match {
      case IntegerConstantType(j) if (i equals j) => true
      case IntegerConstantType(_) => false
      case _ => IntegerType < that
    }
  }
}

/* A dependent type, used to index into records, objects, and the like. */
case class FieldType(f: String) extends Type {

  override def toString = "." + f

  override def <(that: Type): Boolean = {
    that match {
      case FieldType(`f`) => true
      case _ => super.<(that)
    }
  }
}

object StringType extends JavaObjectType(classOf[java.lang.String])
object BooleanType extends JavaObjectType(classOf[java.lang.Boolean])

object NullType extends Type {

  override def toString = "Null"

  override def <(that: Type): Boolean = {
    that.isInstanceOf[JavaObjectType] || super.<(that)
  }

}
