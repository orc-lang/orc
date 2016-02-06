//
// PorcInfixNotation.scala -- Scala class/trait/object PorcInfixNotation
// Project OrcScala
//
// $Id$
//
// Created by amp on May 27, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

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
  def :::(f: Expr): Sequence = Sequence(f, this)
}

object PorcInfixNotation {
  implicit val reflectiveCalls = scala.language.reflectiveCalls

  def let(defs: (Var, Expr)*)(body: Expr): Expr = if (defs.isEmpty) body else Let(defs.head._1, defs.head._2, let(defs.tail: _*)(body))
  def continuation(arg: Var)(body: Expr): Expr = Continuation(arg, body)
}

object LetIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l@Let(x, v, b)) in ctx =>
      val bodyctx = ctx + LetBound(ctx, l)
      Some(x in ctx, v in ctx, b in bodyctx)
    case _ => None
  }
}
object ContinuationIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (l@Continuation(arg, b)) in ctx =>
      val bodyctx = ctx extendBindings List(ContinuationArgumentBound(ctx, l, arg))
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

object SiteIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@Site(ds, b)) in ctx =>
      val bodyctx = ctx extendBindings ds.map(SiteBound(ctx, s, _))
      val sitectx = ctx extendBindings (ds.map(RecursiveSiteBound(ctx, s, _)))
      Some(ds, sitectx, b in bodyctx)
    case _ => None
  }
}

object SiteDefIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case SiteDefDirectIn(n, args, ctx, b) => Some(n, args, ctx, b)
    case SiteDefCPSIn(n, _, _, _, args, ctx, b) => Some(n, args, ctx, b)
    case _ => None
  }
}

object SiteDefCPSIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s : SiteDefCPS) in ctx =>
      val bodyctx = ctx extendBindings (s.arguments :+ s.pArg  :+ s.cArg :+ s.tArg).map(SiteArgumentBound(ctx, s, _))
      Some(s.name, s.pArg, s.cArg, s.tArg, s.arguments, bodyctx, s.body in bodyctx)
    case _ => None
  }
}

object SiteDefDirectIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s : SiteDefDirect) in ctx =>
      val bodyctx = ctx extendBindings s.arguments.map(SiteArgumentBound(ctx, s, _))
      Some(s.name, s.arguments, bodyctx, s.body in bodyctx)
    case _ => None
  }
}


object CallIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (c@Call(t, args)) in ctx => Some(t in ctx, args, ctx)
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


// ==================== PROCESS ===================

object SpawnIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case Spawn(c, t, e) in ctx => Some(c in ctx, t in ctx, e in ctx)
    case _ => None
  }
}

object SpawnFutureIn {
  def unapply(e: WithContext[PorcAST]) = e match {
    case (s@SpawnFuture(c, t, pArg, e)) in ctx =>
      val bodyCtx = ctx extendBindings List(SpawnFutureBound(ctx, s, pArg))
      Some(c in ctx, t in ctx, pArg, e in bodyCtx)
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
    case Force(p, c, f) in ctx => Some(p in ctx, c in ctx, f in ctx)
    case _ => None
  }
}
