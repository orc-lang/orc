//
// ContextualTransform.scala -- Scala class/trait/object ContextualTransform
// Project OrcScala
//
// Created by amp on May 31, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.ast.oil.named.EmptyFunction

trait ContextualTransform {
  def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E

  def apply(c: Expr): Expr = transformExpr(c in TransformContext())
  def apply(c: Value): Value = transformValue(c in TransformContext())
  def apply(c: Def): Def = transformDef(c in TransformContext())
  def apply(c: WithContext[Expr]): Expr = transformExpr(c)
  def apply(c: WithContext[Value]): Value = transformValue(c)
  def apply(c: WithContext[Def]): Def = transformDef(c)

  def apply(c: PorcAST): PorcAST = c match {
    case c: Value => this(c)
    case c: Expr => this(c)
    case c: Def => this(c)
  }
  def apply(c: WithContext[PorcAST]): PorcAST = c match {
    case c @ ((_: Value) in _) => this(c)
    case c @ ((_: Expr) in _) => this(c)
    case c @ ((_: Def) in _) => this(c)
  }

  def onExpr: PartialFunction[WithContext[Expr], Expr] = EmptyFunction
  def onValue: PartialFunction[WithContext[Value], Value] = EmptyFunction
  def onVar: PartialFunction[WithContext[Var], Value] = EmptyFunction

  def callHandler[E <: PorcAST](e: WithContext[E]): Option[E] = {
    e match {
      case (v: Var) in ctx => onVar.lift(v in ctx).asInstanceOf[Option[E]]
      case (v: Value) in ctx => onValue.lift(v in ctx).asInstanceOf[Option[E]]
      case (c: Expr) in ctx => onExpr.lift(c in ctx).asInstanceOf[Option[E]]
      case _ => None
    }
  }

  def transferMetadata[E <: PorcAST](e: E, e1: E): E = {
    (e, e1) match {
      case (_: Value, _: Value) => {
        e1.pushDownPosition(e.sourceTextRange)
        e1
      }
      case _ => e ->> e1
    }
  }

  def transformExpr(expr: WithContext[Expr]): Expr = {
    order[Expr](callHandler, {
      case LetIn(x, v, b) => {
        val e1 = Let(x, transformExpr(v), b)
        val bctx = x.ctx + LetBound(x.ctx, e1)
        Let(e1.x, e1.v, transformExpr(b.e in bctx))
      }
      case s @ DefDeclarationIn(l, ctx, b) => {
        val ls1 = l map { v => transformDef(v in ctx) }
        val e1 = DefDeclaration(ls1, b)
        val ctx1 = s.ctx extendBindings ls1.map(DefBound(ctx, e1, _))
        DefDeclaration(ls1, transformExpr(b.e in ctx1))
      }

      case CallIn(t, a, ctx) => Call(transformValue(t), transformValue(a in ctx))
      case SiteCallIn(target, p, c, t, args, ctx) => SiteCall(transformValue(target), transformValue(p in ctx), transformValue(c in ctx), transformValue(t in ctx), args map { v => transformValue(v in ctx) })
      case SiteCallDirectIn(t, a, ctx) => SiteCallDirect(transformValue(t), a map { v => transformValue(v in ctx) })
      case DefCallIn(target, p, c, t, args, ctx) => DefCall(transformValue(target), transformValue(p in ctx), transformValue(c in ctx), transformValue(t in ctx), args map { v => transformValue(v in ctx) })
      case DefCallDirectIn(t, a, ctx) => DefCallDirect(transformValue(t), a map { v => transformValue(v in ctx) })
      case IfDefIn(a, f, g) => IfDef(transformValue(a), transformExpr(f), transformExpr(g))

      case ContinuationIn(arg, ctx, b) => Continuation(arg in ctx, transformExpr(b))

      case SequenceIn(l, ctx) => {
        Sequence(l map { e => transformExpr(e in ctx) })
      }

      case TryOnKilled(b, h) in ctx => TryOnKilled(transformExpr(b in ctx), transformExpr(h in ctx))
      case TryOnHalted(b, h) in ctx => TryOnHalted(transformExpr(b in ctx), transformExpr(h in ctx))
      case TryFinally(b, h) in ctx => TryFinally(transformExpr(b in ctx), transformExpr(h in ctx))

      case SpawnIn(c, t, e) => Spawn(transformValue(c), transformValue(t), transformExpr(e))
      case SpawnBindFutureIn(f, c, t, pArg, cArg, e) => {
        // TODO: This is actually wrong. IT needs to make an intermediate node for the context of e. Like let does.
        SpawnBindFuture(transformValue(f), transformValue(c), transformValue(t), pArg, cArg, transformExpr(e))
      }

      case NewCounterIn(c, e) => NewCounter(transformValue(c), transformExpr(e))
      case Halt(c) in ctx => Halt(transformValue(c in ctx))
      case SetDiscorporate(c) in ctx => SetDiscorporate(transformValue(c in ctx))

      case NewTerminatorIn(t) => NewTerminator(transformValue(t))
      case KillIn(t) => Kill(transformValue(t))

      case ForceIn(p, c, t, b, vs, ctx) => Force(transformValue(p), transformValue(c), transformValue(t), b, vs.map(v => transformValue(v in ctx)))
      case TupleElem(v, i) in ctx => TupleElem(transformValue(v in ctx), i)

      case NewIn(bindings, ctx) => {
        New(bindings.mapValues(b => transformExpr(b in ctx)))
      }

      case GetField(p, c, t, o, f) in ctx => GetField(transformValue(p in ctx), transformValue(c in ctx), transformValue(t in ctx), transformValue(o in ctx), f)

      case (v: Value) in ctx  => transformValue(v in ctx)

      case e in _ if e.productArity == 0 => e
    })(expr)
  }

  def transformDef(expr: WithContext[Def]): Def = {
    order[Def](callHandler, {
      case DefCPSIn(name, p, c, t, args, ctx, body) =>
        DefCPS(
          name,
          p,
          c,
          t,
          args,
          transformExpr(body))
      case DefDirectIn(name, args, ctx, body) =>
        DefDirect(
          name,
          args,
          transformExpr(body))
    })(expr)
  }

  def transformValue(expr: WithContext[Value]): Value = {
    order[Value](callHandler, {
      case (n @ OrcValue(_)) in _ => n
      //case (n @ Tuple(vs)) in ctx => Tuple(vs.map(v => transformValue(v in ctx)))
      case (v @ Unit()) in _ => v
      case (v: Var) in ctx => v
    })(expr)
  }
  def transformVariable(expr: WithContext[Var]): Var = {
    order[Var](callHandler, x => x)(expr)
  }
}

object ContextualTransform {
  trait NonDescending extends ContextualTransform {
    def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E = {
      val e1 = pf(e) match {
        case Some(e1) => e1
        case None => descend(e)
      }
      e1
      // TODO: Reenable somehow later. It's too expensive, but we will need it eventually.
      //transferMetadata(e, e1)
    }
  }
  trait Pre extends ContextualTransform {
    def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E = {
      val e1 = pf(e) match {
        case Some(e1) => descend(e1 in e.ctx)
        case None => descend(e)
      }
      e1
      // TODO: Reenable somehow later. It's too expensive, but we will need it eventually.
      //transferMetadata(e, e1)
    }
  }
  trait Post extends ContextualTransform {
    def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E = {
      val e0 = descend(e) in e.ctx
      val e1 = pf(e0) match {
        case Some(e1) => e1
        case None => e0.e
      }
      e1
      // TODO: Reenable somehow later. It's too expensive, but we will need it eventually.
      //transferMetadata(e, e1)
    }
  }
}
