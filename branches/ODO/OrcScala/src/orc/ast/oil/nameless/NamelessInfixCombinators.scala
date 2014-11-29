//
// NamelessInfixCombinators.scala -- Scala trait NamelessInfixCombinators
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 31, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.nameless

// Infix combinator constructors
trait NamelessInfixCombinators {
  self: Expression =>

  // Infix combinator constructors
  def ||(g: Expression) = Parallel(this, g)
  def >>(g: Expression) = Sequence(this, g)
  def <<|(g: Expression) = LateBind(this, g)
  def ow(g: Expression) = Otherwise(this, g)
}

// Infix combinator extractors
// Infix combinator extractors
object || {
  def apply(f: Expression, g: Expression) = Parallel(f, g)
  def unapply(e: Expression) =
    e match {
      case Parallel(l, r) => Some((l, r))
      case _ => None
    }
}

object >> {
  def unapply(e: Expression) =
    e match {
      case Sequence(l, r) => Some((l, r))
      case _ => None
    }
}

object <<| {
  def unapply(e: Expression) =
    e match {
      case LateBind(l, r) => Some((l, r))
      case _ => None
    }
}

object ow {
  def unapply(e: Expression) =
    e match {
      case Otherwise(l, r) => Some((l, r))
      case _ => None
    }
}
