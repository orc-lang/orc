//
// TypeConstructor.scala -- Scala class/trait/object TypeConstructor
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

/**
 * 
 *
 * @author dkitchin
 */
trait TypeConstructor {
  val variances: List[Variance]
}

class SimpleTypeConstructor(val name: String, val givenVariances: Variance*) extends TypeConstructor {
  
  override def toString = name
  
  val variances = givenVariances.toList
  
  def apply(ts: Type*): Type = {
    assert(variances.size == ts.size)
    TypeInstance(this, ts.toList)
  }
  
  def unapplySeq(t: Type): Option[Seq[Type]] = {
    t match {
      case TypeInstance(tycon, ts) if (tycon eq this) => Some(ts.toSeq)
      case _ => None
    }
  }
  
}

case class DatatypeConstructor(typeFormals: List[TypeVariable], variants: List[(String, List[Type])]) extends TypeConstructor {
  
  val variances = typeFormals map { x => 
    val occurrences = 
      for ((_, variant) <- variants; t <- variant) yield { 
        t varianceOf x 
      }
    occurrences.toList.combined
  }
  
}



