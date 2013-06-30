//
// TranslateToPorcEval.scala -- Scala class/trait/object TranslateToPorcEval
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 16, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import orc.run.{porc => pe}

/**
  *
  * @author amp
  */
object TranslateToPorcEval {
  def apply(ast: Command): pe.Command = translate(ast)(TranslationContext(Nil))
  
  case class TranslationContext(
      variables: List[Var]) {
    def +(v: Var) = this.copy(variables = v :: variables)
    def ++(s: List[Var]) = this.copy(variables = s ::: variables)
    def apply(v: Var): Int = {
      val i = variables.indexOf(v)
      assert(i >= 0, s"Unknown variable: $v")
      i
    }
  }

  def translate(ast: Value)(implicit ctx: TranslationContext): pe.Value = {
    ast match {
      case Constant(v) => v
      case Tuple(l) => pe.Tuple(l map {translate(_)})
      case v : Var => pe.Var(ctx(v), v.optionalVariableName)
    }
  }

  def translate(d: ClosureDef)(implicit ctx: TranslationContext): pe.ClosureDef = {
    pe.ClosureDef(d.name.optionalVariableName, d.arguments.size, translate(d.body)(ctx ++ d.arguments))
  }
  def translate(d: SiteDef)(implicit ctx: TranslationContext): pe.SiteDef = {
    pe.SiteDef(d.name.optionalVariableName, d.arguments.size, translate(d.body)(ctx ++ d.arguments))
  }
  
  def translate(ast: Command)(implicit ctx: TranslationContext): pe.Command = {
    ast match {
      case Let(d, b) => pe.Let(translate(d), translate(b)(ctx + d.name))
      case Site(l, b) => pe.Site(l.map(d => translate(d)(ctx ++ l.map(_.name))), translate(b)(ctx ++ l.map(_.name)))

      case ClosureCall(t, a) => pe.ClosureCall(translateVarVar(t), a map translate)
      case SiteCall(t, a) => pe.SiteCall(translateVarVar(t), a map translate)
      
      case Unpack(vars, v, k) => pe.Unpack(vars.size, translate(v), translate(k)(ctx ++ vars))
      
      case Spawn(v, k) => pe.Spawn(translateVar(v), translate(k))
      case Die() => pe.Die()
        
      case NewCounter(k) => pe.NewCounter(translate(k))
      case RestoreCounter(a, b) => pe.RestoreCounter(translate(a), translate(b))
      case SetCounterHalt(v, k) => pe.SetCounterHalt(translateVar(v), translate(k))
      case GetCounterHalt(x, k) => pe.GetCounterHalt(translate(k)(ctx + x))
      case DecrCounter(k) => pe.DecrCounter(translate(k))

      case NewTerminator(k) => pe.NewTerminator(translate(k))
      case GetTerminator(x, k) => pe.GetTerminator(translate(k)(ctx + x))
      case Kill(a, b) => pe.Kill(translate(a), translate(b))
      case IsKilled(a, b) => pe.IsKilled(translate(a), translate(b))
      case AddKillHandler(u, m, k) => pe.AddKillHandler(translateVar(u), translateVar(m), translate(k))
      case CallKillHandlers(k) => pe.CallKillHandlers(translate(k))

      case NewFuture(x, k) => pe.NewFuture(translate(k)(ctx + x))
      case Force(vs, a, b) => pe.Force(translate(vs), translateVar(a), translateVar(b))
      case Bind(f, v, k) => pe.Bind(translateVar(f), translate(v), translate(k))
      case Stop(f, k) => pe.Stop(translateVar(f), translate(k))
      
      case NewFlag(x, k) => pe.NewFlag(translate(k)(ctx + x))
      case SetFlag(f, k) => pe.SetFlag(translateVar(f), translate(k))
      case ReadFlag(f, a, b) => pe.ReadFlag(translateVar(f), translate(a), translate(b))

      case ExternalCall(s, args, h, p) => pe.ExternalCall(s, translate(args), translateVar(h), translateVar(p))
      
      case _ => ???
    }
  }
  
  def translateVar(v: Value)(implicit ctx: TranslationContext): Int = {
    v match {
      case v : Var => ctx(v)
    }
  }
  def translateVarVar(v: Value)(implicit ctx: TranslationContext): pe.Var = {
    v match {
      case v : Var => pe.Var(ctx(v), v.optionalVariableName)
    }
  }
  /*def translateTuple(v: Value)(implicit ctx: TranslationContext): List[pe.Value] = {
    v match {
      case Tuple(vs) => vs.map(translate(_:Value))
    }
  }*/
}