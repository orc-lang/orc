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
  
  def apply(c: PorcAST): PorcAST = c match {
    case c: Value => this(c)
    case c: Expr => this(c)
    case c: SiteDef => this(c)
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
        val e1 = Let(transformVariable(x), transformExpr(v), b)
        Let(e1.x, e1.v, transformExpr(b.e in x.ctx + LetBound(x.ctx, e1)))
      }
      case s@SiteIn(l, ctx, b) => {
        val ls1 = l map { v => transformSiteDef(v in ctx) }
        val e1 = Site(ls1, b)
        val ctx1 = s.ctx extendBindings ls1.map(SiteBound(ctx, e1, _))
        Site(ls1, transformExpr(b.e in ctx1))
      }

      case CallIn(t, a, ctx) => Call(transformValue(t), a map { v => transformValue(v in ctx) })
      case SiteCallIn(t, a, p, ctx) => SiteCall(transformValue(t), a map { v => transformValue(v in ctx) }, transformValue(p in ctx))
      case DirectSiteCallIn(t, a, ctx) => DirectSiteCall(transformValue(t), a map { v => transformValue(v in ctx) })

      case LambdaIn(args, ctx, b) => Lambda(args map {v => transformVariable(v in ctx)}, transformExpr(b))

      case SequenceIn(l, ctx) => {
        Sequence(l map {e => transformExpr(e in ctx)})
      }

      case If(b, t, e) in ctx => If(transformValue(b in ctx), transformExpr(t in ctx), transformExpr(e in ctx))
      case TryOnKilled(b, h) in ctx => TryOnKilled(transformExpr(b in ctx), transformExpr(h in ctx))
      case TryOnHalted(b, h) in ctx => TryOnHalted(transformExpr(b in ctx), transformExpr(h in ctx))
      

      //case ProjectIn(n, v) => Project(n, transformValue(v))
      
      case SpawnIn(v) => Spawn(transformVariable(v))
        
      case NewCounterIn(e) => NewCounter(transformExpr(e))
      //case NewCounterDisconnectedIn(k) => NewCounterDisconnected(transformCommand(k))
      case RestoreCounterIn(a, b) => RestoreCounter(transformExpr(a), transformExpr(b))
      case SetCounterHaltIn(v) => SetCounterHalt(transformVariable(v))

      case NewTerminatorIn(k) => NewTerminator(transformExpr(k))
      case AddKillHandlerIn(u, m) => AddKillHandler(transformValue(u), transformValue(m))
      case IsKilledIn(t) => IsKilled(transformVariable(t))
        
      case ForceIn(vs, ctx, b) => Force(vs map (v => transformValue(v in ctx)), transformValue(b))
      case BindIn(f, v) => Bind(transformVariable(f), transformValue(v))
      case StopIn(f) => Stop(transformVariable(f))
      
      case SetFlagIn(f) => SetFlag(transformVariable(f))
      case ReadFlagIn(f) => ReadFlag(transformVariable(f))

      case ExternalCallIn(s, args, ctx, p) => ExternalCall(s, args map (v => transformValue(v in ctx)), transformValue(p))

      case e in _ if e.productArity == 0 => e
    })(expr)
  }
  
  def transformSiteDef(expr: WithContext[SiteDef]): SiteDef = {
    order[SiteDef](callHandler, {
      case SiteDefIn(name, args, pArg, ctx, body) => SiteDef(transformVariable(name in ctx), args map { v => transformVariable(v in ctx) }, transformVariable(pArg in ctx), transformExpr(body))
    })(expr)
  }

  def transformValue(expr: WithContext[Value]): Value = {
    order[Value](callHandler, {
      case (n @ OrcValue(_)) in _ => n
      //case (n @ Tuple(vs)) in ctx => Tuple(vs.map(v => transformValue(v in ctx)))
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
