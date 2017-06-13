//
// PorcInfixNotation.scala -- Scala class/trait/object PorcInfixNotation
// Project OrcScala
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.values.Field

/**
  * @author amp
  */

trait WithContextInfixCombinator {
  self: PorcAST =>
  def in(ctx: TransformContext): WithContext[this.type] = WithContext(this, ctx)
}

object in {
  def unapply[E <: PorcAST](c: WithContext[E]): Option[(E, TransformContext)] = Some((c.e, c.ctx))
}

trait PorcInfixValue {
  this: Value =>
  def apply(arg: Value) = Call(this, arg)
  def sitecall(p: Value, c: Value, t: Value, arg: Value*) = SiteCall(this, p, c, t, arg.toList)
  def sitecalldirect(arg: Value*) = SiteCallDirect(this, arg.toList)
}
trait PorcInfixExpr {
  this: Expr =>
  def :::(f: Expr): Sequence = Sequence(Seq(f, this))
}

object PorcInfixNotation {
  implicit val reflectiveCalls = scala.language.reflectiveCalls

  def let(defs: (Var, Expr)*)(body: Expr): Expr = if (defs.isEmpty) body else Let(defs.head._1, defs.head._2, let(defs.tail: _*)(body))
  def continuation(arg: Var)(body: Expr): Expr = Continuation(arg, body)
}

object LetIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l @ Let(x, v, b)) in ctx =>
      val bodyctx = ctx + LetBound(ctx, l)
      //val valctx = ctx + BoundTo(ctx, l)
      Some(x in ctx, v in bodyctx, b in bodyctx)
    case _ => None
  }
}
object ContinuationIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l @ Continuation(arg, b)) in ctx =>
      val bodyctx = ctx + ContinuationArgumentBound(ctx, l, arg)
      Some(arg, bodyctx, b in bodyctx)
    case _ => None
  }
}

object SequenceIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Sequence(l) in ctx =>
      Some(l, ctx)
    case _ => None
  }
}

object DefDeclarationIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s @ DefDeclaration(ds, b)) in ctx =>
      val bodyctx = ctx extendBindings ds.map(DefBound(ctx, s, _))
      val sitectx = ctx extendBindings ds.map(RecursiveDefBound(ctx, s, _))
      Some(ds, sitectx, b in bodyctx)
    case _ => None
  }
}

object DefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case DefDirectIn(n, args, ctx, b) => Some(n, args, ctx, b)
    case DefCPSIn(n, _, _, _, args, ctx, b) => Some(n, args, ctx, b)
    case _ => None
  }
}

object DefCPSIn {
  type MatchResult = Option[(Var, Var, Var, Var, Seq[Var], TransformContext, WithContext[Expr])]
  def unapply(e: WithContext[PorcAST]): MatchResult = e match {
    case (s: DefCPS) in ctx =>
      val bodyctx = ctx extendBindings (s.arguments :+ s.pArg :+ s.cArg :+ s.tArg).map(DefArgumentBound(ctx, s, _))
      Some(s.name, s.pArg, s.cArg, s.tArg, s.arguments, bodyctx, s.body in bodyctx)
    case _ => None
  }
}

object DefDirectIn {
  type MatchResult = Option[(Var, Seq[Var], TransformContext, WithContext[Expr])]
  def unapply(e: WithContext[PorcAST]): MatchResult = e match {
    case (s: DefDirect) in ctx =>
      val bodyctx = ctx extendBindings s.arguments.map(DefArgumentBound(ctx, s, _))
      Some(s.name, s.arguments, bodyctx, s.body in bodyctx)
    case _ => None
  }
}

object IfDefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c @ IfDef(a, f, g)) in ctx => Some(a in ctx, f in ctx, g in ctx)
    case _ => None
  }
}

object CallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c @ Call(t, args)) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object SiteCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case SiteCall(target, p, c, t, args) in ctx => Some(target in ctx, p, c, t, args, ctx)
    case _ => None
  }
}
object SiteCallDirectIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case SiteCallDirect(t, args) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object DefCallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case DefCall(target, p, c, t, args) in ctx => Some(target in ctx, p, c, t, args, ctx)
    case _ => None
  }
}
object DefCallDirectIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case DefCallDirect(t, args) in ctx => Some(t in ctx, args, ctx)
    case _ => None
  }
}

object TryOnHaltedIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case TryOnHalted(b, h) in ctx => Some(b in ctx, h in ctx)
    case _ => None
  }
}
object TryOnKilledIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case TryOnKilled(b, h) in ctx => Some(b in ctx, h in ctx)
    case _ => None
  }
}
object TryFinallyIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case TryFinally(b, h) in ctx => Some(b in ctx, h in ctx)
    case _ => None
  }
}

object NewIn {
  type MatchResult = Option[(Map[Field, Expr], TransformContext)]
  def unapply(e: WithContext[PorcAST]): MatchResult = e match {
    case (s: New) in ctx =>
      Some(s.bindings, ctx)
    case _ => None
  }
}

// ==================== PROCESS ===================

object SpawnIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Spawn(c, t, e) in ctx => Some(c in ctx, t in ctx, e in ctx)
    case _ => None
  }
}

object SpawnBindFutureIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s @ SpawnBindFuture(f, c, t, pArg, cArg, e)) in ctx =>
      val bodyCtx = ctx extendBindings List(SpawnBindFutureBound(ctx, s, pArg), SpawnBindFutureBound(ctx, s, cArg))
      Some(f in ctx, c in ctx, t in ctx, pArg, cArg, e in bodyCtx)
    case _ => None
  }
}

object NewCounterIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case NewCounter(c, h) in ctx => Some(c in ctx, h in ctx)
    case _ => None
  }
}

object NewTerminatorIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case NewTerminator(t) in ctx => Some(t in ctx)
    case _ => None
  }
}

object KillIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Kill(t) in ctx => Some(t in ctx)
    case _ => None
  }
}

// ==================== FUTURE ===================

object ForceIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Force(p, c, t, b, vs) in ctx => Some(p in ctx, c in ctx, t in ctx, b, vs, ctx)
    case _ => None
  }
}
