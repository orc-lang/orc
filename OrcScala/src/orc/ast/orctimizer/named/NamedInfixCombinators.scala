//
// NamedInfixCombinators.scala -- Scala trait NamedInfixCombinators
// Project OrcScala
//
// Created by dkitchin on May 31, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

// Infix combinator constructors
trait NamedInfixCombinators {
  self: Expression =>

  def ||(g: Expression) = Parallel(this, g)

  def >>(g: Expression) = Branch(this, new BoundVar(), g)

  trait WithGreater {
    def >(g: Expression): Expression
  }
  def >(x: BoundVar): WithGreater =
    new WithGreater {
      def >(g: Expression) = Branch(NamedInfixCombinators.this, x, g)
    }

  def otw(g: Expression) = Otherwise(this, g)
}

// Infix combinator extractors
object || {
  def unapply(e: NamedAST) =
    e match {
      case Parallel(l, r) => Some((l, r))
      case _ => None
    }
}

object > {
  def unapply(e: NamedAST) = {
    e match {
      case Branch(f, x, g) => Some(((f, x), g))
      case _ => None
    }
  }
  def unapply[T](p: (T, BoundVar)) = Some(p)
}
