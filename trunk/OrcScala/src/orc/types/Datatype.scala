//
// Datatype.scala -- Scala trait Datatype and child classes
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 5, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.types.Variance._

// TODO: Add recursion to datatype definitions, 
// e.g. to allow
// type Tree[A] = Leaf | Node(Tree[A], A, Tree[A])


/* Note that neither descendant of Datatype is a case class.
 * This is because we do _not_ want structural equality for
 * datatypes; two datatypes with equivalent constructors are
 * not equal to each other.
 */
trait Datatype {
  val variants: List[(String, List[Type])]
  var optionalDatatypeName: Option[String] = None
}

class MonomorphicDatatype(val variants: List[(String, List[Type])]) extends Type with Datatype {
 
  override def toString = optionalDatatypeName.getOrElse("`datatype")

}


class PolymorphicDatatype(val typeFormals: List[TypeVariable], val variants: List[(String, List[Type])]) extends TypeConstructor with Datatype {
  
  override def toString = optionalDatatypeName.getOrElse("`datatype")
  
  val variances = typeFormals map { x => 
    val occurrences = 
      for ((_, variant) <- variants; t <- variant) yield { 
        t varianceOf x 
      }
    occurrences.toList.combined
  }
  
  def operate(ts: List[Type]): Type = {
    assert(typeFormals.size == ts.size)
    TypeInstance(this, ts)
  }
  
}
