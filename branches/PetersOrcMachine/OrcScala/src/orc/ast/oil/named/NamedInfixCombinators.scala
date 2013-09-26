//
// NamedInfixCombinators.scala -- Scala class/trait/object NamedInfixCombinators
// Project OrcScala
//
// $Id$
//
// Created by amp on Apr 29, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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

  trait with_> {
    def >(g: Expression): Expression
  }

  def >(x: BoundVar) =
    new with_> {
      def >(g: Expression) = Sequence(NamedInfixCombinators.this, x, g)
    }

  def <<|(g: Expression) = LateBind(this, new BoundVar(), g)

  trait with_<| {
    def <|(g: Expression): Expression
  }

  def <(x: BoundVar) =
    new with_<| {
      def <|(g: Expression) = LateBind(NamedInfixCombinators.this, x, g)
    }

  def ow(g: Expression) = Otherwise(this, g)
}

// Infix combinator extractors
object || {
  def unapply(e: NamedAST) =
    e match {
      case Parallel(l, r) => Some((l, r))
      case _ => None
    }
  def unapply(e: WithContext[NamedAST]) =
    e match {
      case Parallel(l, r) in ctx => Some((l in ctx, r in ctx))
      case _ => None
    }
}

object > {
  def unapply(e: NamedAST) = {
    e match {
      case Sequence(f, x, g) => Some(((f, x), g))
      case _ => None
    }
  }
  def unapply(e: WithContext[NamedAST]) = {
    e match {
      case (n@Sequence(f, x, g)) in ctx => {
        val newctx = ctx + Bindings.SeqBound(ctx, n)
        Some(((f in ctx, x), g in newctx))
      }
      case _ => None
    }
  }
  def unapply[A, B](p: (A, B)) = Some(p)
}


object <| {
  def unapply(e: NamedAST) = {
    e match {
      case LateBind(f, x, g) => Some(((f, x), g))
      case _ => None
    }
  }
  def unapply(e: WithContext[NamedAST]) = {
    e match {
      case (n@LateBind(f, x, g)) in ctx => {
        val newctx = ctx + Bindings.LateBound(ctx, n)
        Some(((f in newctx, x), g in ctx))
      }
      case _ => None
    }
  }
}
object < {
  def unapply[A, B](p: (A, B)) = Some(p)
}


object ow {
  def unapply(e: NamedAST) =
    e match {
      case Otherwise(l, r) => Some((l, r))
      case _ => None
    }
  def unapply(e: WithContext[NamedAST]) =
    e match {
      case Otherwise(l, r) in ctx => Some((l in ctx, r in ctx))
      case _ => None
    }
}

object DeclareDefsAt {
  def unapply(e: WithContext[Expression]) = e match {
    case (n@DeclareDefs(defs, body)) in ctx => {
      val defsctx = ctx extendBindings (defs map { Bindings.RecursiveDefBound(ctx, n, _) })
      val bodyctx = ctx extendBindings (defs map { Bindings.DefBound(defsctx, n, _) })
      Some(defs, defsctx, body in bodyctx)
    }
    case _ => None
  }
}
object DefAt {
  def unapply(e: WithContext[NamedAST]) = e match {
    case (d@Def(name, formals, body, typeformals, argtypes, returntype)) in ctx => {
      val newctx = ctx extendBindings (formals map { Bindings.ArgumentBound(ctx, d, _) }) extendTypeBindings (typeformals map { TypeBinding(ctx, _) })
      Some(name in ctx, formals, body in newctx, typeformals, argtypes, returntype, newctx)
    }
    case _ => None
  }
}
object LimitAt {
  def unapply(e: WithContext[Expression]) = e match {
    case (n@Limit(f)) in ctx => {
      Some(f in ctx)
    }
    case _ => None
  }
}
object CallAt {
  def unapply(e: WithContext[Expression]) = e match {
    case (n@Call(t, args, targs)) in ctx => {
      Some(t in ctx, args, targs, ctx)
    }
    case _ => None
  }
}
object DeclareTypeAt {
  def unapply(e: WithContext[Expression]) = e match {
    case (n@DeclareType(tv, t, b)) in ctx => {
      val newctx = ctx + TypeBinding(ctx, tv)
      Some(tv in newctx, t in newctx, b in newctx)
    }
    case _ => None
  }
}
