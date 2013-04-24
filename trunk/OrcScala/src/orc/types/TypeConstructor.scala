//
// TypeConstructor.scala -- Scala trait TypeConstructor
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 28, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import scala.language.existentials
import orc.types.Variance._
import orc.error.compiletime.typing.UncallableTypeException
import orc.error.compiletime.typing.SecondOrderTypeExpectedException
import orc.compile.typecheck.Typeloader

/**
  *
  * @author dkitchin
  */
trait TypeConstructor extends TypeOperator {

  val variances: List[Variance]

  def operate(ts: List[Type]): Type = {
    assert(variances.size == ts.size)
    TypeInstance(this, ts)
  }

  /* 
   * When an instance of this type is called, instantiate it at particular type parameters.
   * By default, a constructed type is uncallable.
   * Subclasses will override this method to provide the calling type behavior of instances.
   */
  def instance(ts: List[Type]): Type = {
    throw new UncallableTypeException(TypeInstance(this, ts))
  }

}

/* Convenience class for the common case of type constructor usage */
class SimpleTypeConstructor(val name: String, val givenVariances: Variance*) extends TypeConstructor {

  override def toString = name

  val variances = givenVariances.toList

  def unapplySeq(t: Type): Option[Seq[Type]] = {
    t match {
      case TypeInstance(tycon, ts) if (tycon eq this) => Some(ts.toSeq)
      case _ => None
    }
  }

}

case class JavaTypeConstructor(cl: Class[_])
  extends SimpleTypeConstructor(cl.getName(), (for (_ <- cl.getTypeParameters()) yield Invariant): _*) {
  val formals = cl.getTypeParameters().toList

  if (formals.isEmpty) {
    throw new SecondOrderTypeExpectedException(Option(cl.getClass.getCanonicalName).getOrElse(cl.getClass.getName))
  }

  override def instance(actuals: List[Type]): Type = {
    Typeloader.liftJavaType(cl, (formals zip actuals).toMap)
  }

  override def <(that: TypeOperator) = {
    that match {
      case JavaTypeConstructor(otherCl) => otherCl isAssignableFrom cl
      case _ => super.<(that)
    }
  }

}
