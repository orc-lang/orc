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

import orc.ast.oil.named.orc5c.EmptyFunction

trait ContextualTransform {  
  def order[E <: PorcAST](pf: WithContext[E] => Option[E], descend: WithContext[E] => E)(e: WithContext[E]): E
  
  def apply(c: Command): Command = transformCommand(c in TransformContext())
  def apply(c: Value): Value = transformValue(c in TransformContext())
  def apply(c: ClosureDef): ClosureDef = transformLetDef(c in TransformContext())
  def apply(c: SiteDef): SiteDef = transformSiteDef(c in TransformContext())
  
  def apply(c: PorcAST): PorcAST = c match {
    case c: Command => this(c)
    case c: Value => this(c)
    case c: ClosureDef => this(c)
    case c: SiteDef => this(c)
  }

  def onCommand: PartialFunction[WithContext[Command], Command] = EmptyFunction
  def onValue: PartialFunction[WithContext[Value], Value] = EmptyFunction
  def onVar: PartialFunction[WithContext[Var], Value] = EmptyFunction

  def callHandler[E <: PorcAST](e: WithContext[E]): Option[E] = {
    e match {
      case (c: Command) in ctx => onCommand.lift(c in ctx).asInstanceOf[Option[E]]
      case (v: Var) in ctx => onVar.lift(v in ctx).asInstanceOf[Option[E]]
      case (v: Value) in ctx => onValue.lift(v in ctx).asInstanceOf[Option[E]]
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
  
  def transformCommand(expr: WithContext[Command]): Command = {
    order[Command](callHandler, {
      case LetIn(d, b) => {
        val l = transformLetDef(d)
        val e1 = Let(l, b)
        Let(l, transformCommand(b.e in d.ctx + LetBound(d.ctx, e1)))
      }
      case s@SiteIn(l, ctx, b) => {
        val ls1 = l map { v => transformSiteDef(v in ctx) }
        val e1 = Site(ls1, b)
        val ctx1 = s.ctx extendBindings ls1.map(SiteBound(ctx, e1, _))
        Site(ls1, transformCommand(b.e in ctx1))
      }

      case ClosureCallIn(t, a, ctx) => ClosureCall(transformValue(t), a map { v => transformValue(v in ctx) })
      case SiteCallIn(t, a, ctx) => SiteCall(transformValue(t), a map { v => transformValue(v in ctx) })
      
      case UnpackIn(vars, v, k) => Unpack(vars map { v => transformVariable(v in k.ctx) }, transformValue(v in k.ctx), transformCommand(k))
      
      case SpawnIn(v, k) => Spawn(transformClosureVariable(v), transformCommand(k))
      case Die() in _ => Die()
        
      case NewCounterIn(k) => NewCounter(transformCommand(k))
      case NewCounterDisconnectedIn(k) => NewCounterDisconnected(transformCommand(k))
      case RestoreCounterIn(a, b) => RestoreCounter(transformCommand(a), transformCommand(b))
      case SetCounterHaltIn(v, k) => SetCounterHalt(transformClosureVariable(v in k.ctx), transformCommand(k))
      case GetCounterHaltIn(x, k) => GetCounterHalt(transformClosureVariable(x in k.ctx), transformCommand(k))
      case DecrCounterIn(k) => DecrCounter(transformCommand(k))

      case NewTerminatorIn(k) => NewTerminator(transformCommand(k))
      case GetTerminatorIn(x, k) => GetTerminator(transformVariable(x in k.ctx), transformCommand(k))
      case KillIn(a, b) => Kill(transformCommand(a), transformCommand(b))
      case IsKilledIn(a, b) => IsKilled(transformCommand(a), transformCommand(b))
      case AddKillHandlerIn(u, m, k) => AddKillHandler(transformValue(u), transformClosureVariable(m), transformCommand(k))
      case CallKillHandlersIn(k) => CallKillHandlers(transformCommand(k))
        
      case NewFutureIn(x, k) => NewFuture(transformVariable(x in k.ctx), transformCommand(k))
      case ForceIn(vs, a, b) => Force(transformValue(vs), transformValue(a), transformValue(b))
      case BindIn(f, v, k) => Bind(transformVariable(f), transformValue(v), transformCommand(k))
      case StopIn(f, k) => Stop(transformVariable(f), transformCommand(k))
      
      case NewFlagIn(x, k) => NewFlag(transformVariable(x in k.ctx), transformCommand(k))
      case SetFlagIn(f, k) => SetFlag(transformVariable(f), transformCommand(k))
      case ReadFlagIn(f, a, b) => ReadFlag(transformVariable(f), transformCommand(a), transformCommand(b))

      case ExternalCallIn(s, args, h, p) => ExternalCall(s, transformValue(args), transformValue(h), transformValue(p))
      })(expr)
  }
  
  def transformLetDef(expr: WithContext[ClosureDef]): ClosureDef = { 
    order[ClosureDef](callHandler, {
      case ClosureDefIn(name, args, ctx, body) => ClosureDef(transformClosureVariable(name in ctx), args map { v => transformVariable(v in ctx) }, transformCommand(body))
    })(expr)
  }
  def transformSiteDef(expr: WithContext[SiteDef]): SiteDef = {
    order[SiteDef](callHandler, {
      case SiteDefIn(name, args, ctx, body) => SiteDef(transformSiteVariable(name in ctx), args map { v => transformVariable(v in ctx) }, transformCommand(body))
    })(expr)
  }

  def transformValue(expr: WithContext[Value]): Value = {
    order[Value](callHandler, {
      case (n @ Constant(_)) in _ => n
      case (n @ Tuple(vs)) in ctx => Tuple(vs.map(v => transformValue(v in ctx)))
      case (v: Variable) in ctx => v
      case (v: SiteVariable) in ctx => v
      case (v: ClosureVariable) in ctx => v
    })(expr)
  }
  def transformVariable(expr: WithContext[Variable]): Variable = {
    order[Variable](callHandler, x => x)(expr)
  }
  def transformSiteVariable(expr: WithContext[SiteVariable]): SiteVariable = {
    order[SiteVariable](callHandler, x => x)(expr)
  }
  def transformClosureVariable(expr: WithContext[ClosureVariable]): ClosureVariable = {
    order[ClosureVariable](callHandler, x => x)(expr)
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
