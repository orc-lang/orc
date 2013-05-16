//
// Optimizer.scala -- Scala class/trait/object Optimizer
// Project OrcScala
//
// $Id$
//
// Created by amp on May 13, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c


trait Optimization extends PartialFunction[(Expression, ExpressionAnalysisStore), Expression] {
  def apply(e : Expression, analysis : ExpressionAnalysisStore) : Expression = apply((e, analysis))
  def name : String
}

case class Opt(name : String)(f : PartialFunction[(Expression, ExpressionAnalysisStore), Expression]) extends Optimization {
  def apply(v : (Expression, ExpressionAnalysisStore)) = f(v)
  def isDefinedAt(v : (Expression, ExpressionAnalysisStore)) = f.isDefinedAt(v)
}

/**
  *
  * @author amp
  */
class Optimizer(opts : Seq[Optimization]) {
  def apply(e : Expression, analysis : ExpressionAnalysisStore) : Expression = {
    val e1 = e match {
      case f || g => apply(f, analysis) || apply(g, analysis)
      case f ow g => apply(f, analysis) ow apply(g, analysis)
      case f > x > g => apply(f, analysis) > x > apply(g, analysis)
      case f < x <| g => apply(f, analysis) < x <| apply(g, analysis)
      case Limit(f) => Limit(apply(f, analysis))
      case DeclareDefs(defs, body) => {
        DeclareDefs(defs.map(d => d.copy(body = apply(d.body, analysis))), apply(body, analysis))
      }
      case d@DeclareType(_, _, b) => d.copy(body = apply(d.body, analysis))
      case d@HasType(b, _) => d.copy(body = apply(d.body, analysis))
      case _ => e
    }
    opts.foldLeft(e1)((e, opt) => {
      opt.lift((e, analysis)) match {
        case None => e
        case Some(e2) => 
          if( e != e2 ) {
            println(s"${opt.name}: ${e.toString.replace("\n", " ").take(60)} ==> ${e2.toString.replace("\n", " ").take(60)}")
            //println(s"${opt.name}: ${e.toString} ==> ${e2.toString}")
            e2
          } else
            e
      }
    })
  }
}

object Optimizer {
  val LateBindElim = Opt("late-bind-elim") {
    case (f < x <| g, a) if a(f).strictOn(x) && a(g).publishesAtMost(1) => g > x > f
    case (f < x <| g, a) if a(g).immediatePublish && a(g).publishesAtMost(1) => g > x > f
    case (Stop() < x <| g, a) => g > x > Stop()
  }
  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if a(f).silent && a(f).effectFree => Stop()
  }
  val LimitElim = Opt("limit-elim") {
    case (Limit(f), a) if a(f).publishesAtMost(1) && a(f).effectFree => f
  }
  val SeqElim = Opt("seq-elim") {
    case (f > x > g, a) if a(f).silent => f
    case (f > x > y, a) if x == y => f
  }
  val ParElim = Opt("par-elim") {
    case (Stop() || g, a) => g
    case (f || Stop(), a) => f
  }
  val OWElim = Opt("otherwise-elim") {
    case (f ow g, a) if a(f).talkative => f
    case (Stop() ow g, a) => g
  }
  val ConstProp = Opt("constant-propogation") {
    case (g < x <| (y : Argument), a) => g.subst(y, x)
    case ((y : Constant) > x > g, a) => g.subst(y, x)
    case ((y : Argument) > x > g, a) if a(y).immediatePublish => g.subst(y, x)
    // Propogation of variables through seq is incorrect because of the blocking provided by a variable reference
    // But if the variable publishs immediate than the blocking would not happen anyway.
  }
  
  private def pars(p : Expression) : Set[Expression] = {
    p match {
      case f || g => pars(f) ++ pars(g)
      case e => Set(e)
    }
  }
  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(p < x <| g), a) if pars(p).exists(!_.freevars.contains(x)) => {
      val (f, h) = pars(p).partition(_.freevars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (f.reduce(_ || _) < x <| g) || h.reduce(_ || _)
        case (true, false) => (g >> Stop()) || h.reduce(_ || _)
        case (false, true) => e
      }
    }
  }
  
  val defaultOptimizer = new Optimizer(List(LiftUnrelated, LimitElim, ConstProp, StopEquiv, LateBindElim, ParElim, OWElim, SeqElim))
}

