//
// TypeConstructor.scala -- Scala trait TypeConstructor
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.types.Variance._
import orc.error.compiletime.typing.UncallableTypeException

/**
 * 
 *
 * @author dkitchin
 */
trait TypeConstructor extends TypeOperator {
  
  val variances: List[Variance]
  
  /* 
   * When an instance of this type is called, instantiate it at particular type parameters.
   * By default, a constructed type is uncallable.
   * Subclasses will override this method to provide the calling type behavior of instances.
   */
  def instance(ts: List[Type]): CallableType = {
    throw new UncallableTypeException(TypeInstance(this, ts))
  }
}

class SimpleTypeConstructor(val name: String, val givenVariances: Variance*) extends TypeConstructor {
  
  override def toString = name
  
  val variances = givenVariances.toList
    
  def operate(ts: List[Type]): Type = {
    assert(variances.size == ts.size)
    TypeInstance(this, ts)
  }
  
  def apply(ts: Type*): Type = {
    operate(ts.toList)
  }
  
  def unapplySeq(t: Type): Option[Seq[Type]] = {
    t match {
      case TypeInstance(tycon, ts) if (tycon eq this) => Some(ts.toSeq)
      case _ => None
    }
  }
  
}
