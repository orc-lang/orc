//
// ContextualTransform.scala -- Scala class/trait/object ContextualTransform
// Project OrcScala
//
// $Id$
//
// Created by amp on May 31, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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
  def apply(c: SiteDef): SiteDef = transformSiteDef(c in TransformContext())
  def apply(c: WithContext[Expr]): Expr = transformExpr(c)
  def apply(c: WithContext[Value]): Value = transformValue(c)
  def apply(c: WithContext[SiteDef]): SiteDef = transformSiteDef(c)
  
  def apply(c: PorcAST): PorcAST = c match {
    case c: Value => this(c)
    case c: Expr => this(c)
    case c: SiteDef => this(c)
  }
  def apply(c: WithContext[PorcAST]): PorcAST = c match {
    case c@((_: Value) in _) => this(c)
    case c@((_: Expr) in _) => this(c)
    case c@((_: SiteDef) in _) => this(c)
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
        e1.pushDownPosition(e.pos)
        e1
      }
      case _ => e ->> e1
    }
  }
  
  def transformExpr(expr: WithContext[Expr]): Expr = {
    order[Expr](callHandler, {
      case LetIn(x, v, b) => {
        val e1 = Let(x, transformExpr(v), b)
        Let(e1.x, e1.v, transformExpr(b.e in x.ctx + LetBound(x.ctx, e1)))
      }
      case s@SiteIn(l, ctx, b) => {
        val ls1 = l map { v => transformSiteDef(v in ctx) }
        val e1 = Site(ls1, b)
        val ctx1 = s.ctx extendBindings ls1.map(SiteBound(ctx, e1, _))
        Site(ls1, transformExpr(b.e in ctx1))
      }

      case CallIn(t, a, ctx) => Call(transformValue(t), transformValue(a in ctx))
      case SiteCallIn(target, p, c, t, args, ctx) => SiteCall(transformValue(target), transformValue(p in ctx), transformValue(c in ctx), transformValue(t in ctx), args map { v => transformValue(v in ctx) })
      case SiteCallDirectIn(t, a, ctx) => SiteCallDirect(transformValue(t), a map { v => transformValue(v in ctx) })

      case ContinuationIn(arg, ctx, b) => Continuation(arg in ctx, transformExpr(b))

      case SequenceIn(l, ctx) => {
        Sequence(l map {e => transformExpr(e in ctx)})
      }

      case TryOnKilled(b, h) in ctx => TryOnKilled(transformExpr(b in ctx), transformExpr(h in ctx))
      case TryOnHalted(b, h) in ctx => TryOnHalted(transformExpr(b in ctx), transformExpr(h in ctx))
      case TryFinally(b, h) in ctx => TryFinally(transformExpr(b in ctx), transformExpr(h in ctx))
      
      
      case SpawnIn(c, t, e) => Spawn(transformValue(c), transformValue(t), transformExpr(e))
      case SpawnFutureIn(c, t, pArg, cArg, e) => {
        // TODO: This is actually wrong. IT needs to make an intermediate node for the context of e. Like let does.
        SpawnFuture(transformValue(c), transformValue(t), pArg, cArg, transformExpr(e))
      }
        
      case NewCounterIn(c, e) => NewCounter(transformValue(c), transformExpr(e))
      case Halt(c) in ctx => Halt(transformValue(c in ctx))

      case NewTerminatorIn(t) => NewTerminator(transformValue(t))
      case KillIn(t) => Kill(transformValue(t))
        
      case ForceIn(p, c, f) => Force(transformValue(p), transformValue(c), transformValue(f))
      //case ResolveIn(f, b) => Resolve(transformValue(f), transformValue(b))

      case GetField(p, c, t, o, f) in ctx => GetField(transformValue(p in ctx), transformValue(c in ctx), transformValue(t in ctx), transformValue(o in ctx), f)
      
      case e in _ if e.productArity == 0 => e
    })(expr)
  }
  
  def transformSiteDef(expr: WithContext[SiteDef]): SiteDef = {
    order[SiteDef](callHandler, {
      case SiteDefCPSIn(name, p, c, t, args, ctx, body) => 
        SiteDefCPS(
            name, 
            p,
            c,
            t,
            args, 
            transformExpr(body))
      case SiteDefDirectIn(name, args, ctx, body) => 
        SiteDefDirect(
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
      transferMetadata(e, e1)
    }
  }
  trait Pre extends ContextualTransform {
    def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E = {
      val e1 = pf(e) match {
        case Some(e1) => descend(e1 in e.ctx)
        case None => descend(e)
      }
      transferMetadata(e, e1)
    }
  }
  trait Post extends ContextualTransform {
    def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E = {
      val e0 = descend(e) in e.ctx
      val e1 = pf(e0) match {
        case Some(e1) => e1
        case None => e0.e
      }
      transferMetadata(e, e1)
    }
  }
}
