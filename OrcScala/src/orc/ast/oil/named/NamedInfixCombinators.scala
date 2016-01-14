//
// NamedInfixCombinators.scala -- Scala trait NamedInfixCombinators
// Project OrcScala
//
// Created by dkitchin on May 31, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.named

// Infix combinator constructors
trait NamedInfixCombinators {
  self: Expression =>

  def ||(g: Expression) = Parallel(this, g)

  def >>(g: Expression) = Sequence(this, new BoundVar(), g)

  def >(x: BoundVar) =
    new {
      def >(g: Expression) = Sequence(NamedInfixCombinators.this, x, g)
    }

  def <<|(g: Expression) = LateBind(this, new BoundVar(), g)

  def <(x: BoundVar) =
    new {
      def <|(g: Expression) = LateBind(NamedInfixCombinators.this, x, g)
    }

  def ow(g: Expression) = Otherwise(this, g)
}

// Infix combinator extractors
object || {
  def unapply(e: Expression) =
    e match {
      case Parallel(l, r) => Some((l, r))
      case _ => None
    }
}

object > {
  def unapply(e: Expression) = {
    e match {
      case Sequence(f, x, g) => Some(((f, x), g))
      case _ => None
    }
  }
  def unapply(p: (Expression, BoundVar)) = Some(p)
}

object <| {
  def unapply(e: Expression) = {
    e match {
      case LateBind(f, x, g) => Some(((f, x), g))
      case _ => None
    }
  }
}

object < {
  def unapply(p: (Expression, BoundVar)) = Some(p)
}

object ow {
  def unapply(e: Expression) =
    e match {
      case Otherwise(l, r) => Some((l, r))
      case _ => None
    }
}
