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
  def apply(ast: Expr): pe.Expr = translate(ast)(TranslationContext(Nil))
  
  case class TranslationContext(
      variables: List[Var]) {
    def +(v: Var) = this.copy(variables = v :: variables)
    def ++(s: List[Var]) = this.copy(variables = s ::: variables)
    def apply(v: Var): Int = {
      val i = variables.indexOf(v)
      //println(s"$v $variables $i ${variables.filter(_ == v)}")
      assert(i >= 0, s"Unknown variable: $v")
      i
    }
  }

  def translate(ast: Value)(implicit ctx: TranslationContext): pe.Value = {
    translateVarVar(ast)
  }


  def translate(d: SiteDef)(implicit ctx: TranslationContext): pe.SiteDef = {
    val bT = translate(d.body)(ctx ++ d.arguments + d.pArg)
    pe.SiteDef(d.name.optionalVariableName, d.arguments.size, bT)
  }
  
  def translate(ast: Expr)(implicit ctx: TranslationContext): pe.Expr = {
    ast match {
      case Let(x, v, b) => pe.Let(translate(v), translate(b)(ctx + x))
      case Site(l, b) => pe.Site(l.map(d => translate(d)(ctx ++ l.map(_.name))), translate(b)(ctx ++ l.map(_.name)))
      case Lambda(arguments, body) => pe.Lambda(arguments.size, translate(body)(ctx ++ arguments))

      case Call(t, a) => 
        pe.Call(translateVarVar(t), a map translate)
      case SiteCall(t, a, p) => 
        pe.SiteCall(translateVarVar(t), a map translate, translate(p))
      case DirectSiteCall(t, a) => 
        pe.DirectSiteCall(translateVarVar(t), a map translate)
            
      case Spawn(v) => pe.Spawn(translateVarVar(v)) 
      case Sequence(es) => pe.Sequence(es map translate) 
      
      case If(b, t, e) => pe.If(translateVarVar(b), translate(t), translate(e))
      
      case CallCounterHalt() => pe.CallCounterHalt
      case CallParentCounterHalt() => pe.CallParentCounterHalt

      case NewCounter(k) => pe.NewCounter(translate(k))
      //case NewCounterDisconnected(k) => pe.NewCounterDisconnected(translate(k))
      case RestoreCounter(a, b) => pe.RestoreCounter(translate(a), translate(b))
      case SetCounterHalt(v) => pe.SetCounterHalt(translateVarVar(v))
      case DecrCounter() => pe.DecrCounter

      case NewTerminator(k) => pe.NewTerminator(translate(k))
      case GetTerminator() => pe.GetTerminator
      case SetKill() => pe.SetKill
      case CheckKilled() => pe.CheckKilled
      case AddKillHandler(u, m) => pe.AddKillHandler(translateVarVar(u), translateVarVar(m))
      case CallKillHandlers() => pe.CallKillHandlers
      case Killed() => pe.Killed
      case TryOnKilled(body, handler) => pe.TryOnKilled(translate(body), translate(handler))
      case Halted() => pe.Halted
      case TryOnHalted(body, handler) => pe.TryOnHalted(translate(body), translate(handler))
      
      case NewFuture() => pe.NewFuture
      case Force(vs, a) => pe.Force(vs map {x => translate(x)}, translateVarVar(a))
      case Bind(f, v) => pe.Bind(translateVarVar(f), translateVarVar(v))
      case Stop(f) => pe.Stop(translateVarVar(f))
      
      case NewFlag() => pe.NewFlag
      case SetFlag(f) => pe.SetFlag(translateVarVar(f))
      case ReadFlag(f) => pe.ReadFlag(translateVarVar(f))

      case ExternalCall(s, args, p) => pe.ExternalCall(s, args map {x => translate(x)}, translateVarVar(p))
      
      case v : Value => pe.ValueExpr(translateVarVar(v))
      case _ => throw new NotImplementedError(s"$ast")
    }
  }
  
  def translateVarVar(v: Value)(implicit ctx: TranslationContext): pe.Value = {
    v match {
      case v : Var => pe.Var(ctx(v), v.optionalVariableName)
      case Unit() => scala.Unit
      case Bool(v) => v : java.lang.Boolean
      case OrcValue(v) => v
    }
  }
}
