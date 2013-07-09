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

import orc.lib.builtin.GetField
import orc.values.OrcRecord
import orc.values.Field
import orc.lib.builtin.ProjectClosure
import orc.lib.builtin.ProjectUnapply


trait Optimization extends ((WithContext[Expression], ExpressionAnalysisProvider[Expression]) => Option[Expression]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name : String
}

case class Opt(name : String)(f : PartialFunction[(WithContext[Expression], ExpressionAnalysisProvider[Expression]), Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f.lift((e, analysis))
}
case class OptSimple(name : String)(f : PartialFunction[WithContext[Expression], Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f.lift(e)
}
case class OptFull(name : String)(f : (WithContext[Expression], ExpressionAnalysisProvider[Expression]) => Option[Expression]) extends Optimization {
  def apply(e : WithContext[Expression], analysis : ExpressionAnalysisProvider[Expression]) : Option[Expression] = f(e, analysis)
}

/**
  *
  * @author amp
  */
case class Optimizer(opts : Seq[Optimization]) {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression]) : Expression = {
    val trans = new ContextualTransform.Pre {
      override def onExpressionCtx = {
        case (e: WithContext[Expression]) => {
          //println("Optimizing: " + (new PrettyPrint).reduce(e))
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.e != e2) {
                  println(s"${opt.name}: ${e.e.toString.replace("\n", " ").take(60)} ==> ${e2.toString.replace("\n", " ").take(60)}")
                  //println(s"${opt.name}: ${e.toString} ==> ${e2.toString}")
                  e2 in e.ctx
                } else
                  e
            }
          })
          e1.e
        }
      }
    }
    
    trans(e)
  }
}

object Optimizer {
  import WithContext._
  
  object Pars {
    private def pars(p: Expression): Set[Expression] = {
      p match {
        case f || g => pars(f) ++ pars(g)
        case e => Set(e)
      }
    }
    def unapply(e: Expression): Option[Set[Expression]] = Some(pars(e))
    def unapply(e: WithContext[Expression]): Option[(Set[Expression], TransformContext)] = {
      Some(pars(e.e), e.ctx)
    }
    
    def apply(l : Traversable[Expression]) = l.reduce(_ || _)
  }
  
  object LateBinds {
    private def latebinds(p: Expression): (Expression, List[(BoundVar, Expression)]) = {
      p match {
        case f < x <| g => {
          val (e1, lbs) = latebinds(f)
          (e1, lbs :+ (x, g))
        }
        case e => (e, Nil)
      }
    }
    private def latebindsAt(p: WithContext[Expression]): (WithContext[Expression], List[(BoundVar, WithContext[Expression])]) = {
      p match {
        case f < x <| g => {
          val (e1, lbs) = latebindsAt(f)
          (e1, lbs :+ (x, g))
        }
        case e => (e, Nil)
      }
    }
    def unapply(e: Expression): Option[(Expression, List[(BoundVar, Expression)])] = Some(latebinds(e))
    def unapply(e: WithContext[Expression]): Option[(WithContext[Expression], List[(BoundVar, WithContext[Expression])])] = {
      Some(latebindsAt(e))
    }
    
    def apply(e : Expression, lbs : List[(BoundVar, Expression)]) : Expression = {
      lbs match {
        case Nil => e
        case (x, g) :: lbst => apply(e < x <| g, lbst)
      }
    }
  }
  
  /**
   * Match an expression in the form: e <x1<| g1 <x2<| g2 ... where x1,...,xn are distinct
   * and no gi references any xi.  
   */
  object IndependantLateBinds {
    private def indepentant(lbs : List[(BoundVar, Expression)]) : Boolean = {
      val vars = lbs.map(_._1).toSet
      val freevars = lbs.flatMap(_._2.freevars).toSet
      vars.size == lbs.size && 
      vars.forall(v => !freevars.contains(v))
    } 
    
    def unapply(e: Expression): Option[(Expression, List[(BoundVar, Expression)])] = {
      e match {
        case LateBinds(e, lbs) if lbs.size > 0 && indepentant(lbs) => Some(e, lbs)
        case e => None
      }
    }
    def unapply(e: WithContext[Expression]): Option[(WithContext[Expression], List[(BoundVar, WithContext[Expression])])] = {
      e match {
        case LateBinds(e, lbs) if lbs.size > 0 && indepentant(lbs.map(p => (p._1, p._2.e))) => Some(e, lbs)
        case e => None
      }
    }
  }
  
  /*val Test = Opt("Test") {
    case (e@IndependantLateBinds(f, lbs), a) => println(s"Test: $f \n<<|\n $lbs\n........"); e
  }*/

  val LateBindReorder = Opt("late-bind-reorder") {
    case (IndependantLateBinds(f, lbs), a) if lbs.exists(p => a(f).strictOn(p._1)) => {
      val lbs1 = lbs.sortBy(p => !a(f).strictOn(p._1))
      LateBinds(f, lbs1.map(p => (p._1, p._2.e)))
    }
  }
  val flattenThreshold = 5
  val LateBindElimFlatten = Opt("late-bind-elim-flatten") {
    case (f < x <| g, a) if a(f).forces(x) && a(g).publishesAtMost(1) && Analysis.cost(g) <= flattenThreshold => g > x > f
  }
  val LateBindElim = Opt("late-bind-elim") {
    case (f < x <| g, a) if a(f).strictOn(x) && a(g).publishesAtMost(1) => g > x > f
    case (f < x <| g, a) if a(g).immediatePublish && a(g).publishesAtMost(1) => g > x > f
    case ((Stop() in _) < x <| g, a) => g > x > Stop()
  }
  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f != Stop() && a(f).silent && a(f).effectFree && a(f).immediateHalt => Stop()
  }
  val SeqElim = Opt("seq-elim") {
    case (f > x > g, a) if a(f).silent => f.e
  }
  val SeqExp = Opt("seq-expansion") {
    case (e@(Pars(fs, ctx) > x > g), a) if fs.exists(f => a(f in ctx).silent) => {
      // This doesn't really eliminate any code and I cannot think of a case where 
      val (sil, nsil) = fs.partition(f => a(f in ctx).silent)
      (sil.isEmpty, nsil.isEmpty) match {
        case (false, false) => (Pars(nsil) > x > g) || Pars(sil)
        case (false, true) => Pars(sil)
        case (true, false) => e
      }
    }
    case (e@(Pars(fs, ctx) > x > g), a) if fs.exists(f => f.isInstanceOf[Constant]) => {
      val cs = fs.collect{ case c : Constant => c }
      val es = fs.filter(f => !f.isInstanceOf[Constant])
      (cs.isEmpty, es.isEmpty) match {
        case (false, false) => (Pars(es) > x > g.e) || Pars(cs.map(g.e.subst(_, x)))
        case (false, true) => Pars(cs.map(g.e.subst(_, x)))
        case (true, false) => e.e
      }
    }
  }
  val SeqElimVar = Opt("seq-elim-var") {
    case (f > x > y, a) if x == y => f.e
  }
  val ParElim = OptSimple("par-elim") {
    case (Stop() in _) || g => g.e
    case f || (Stop() in _) => f.e
  }
  val OWElim = Opt("otherwise-elim") {
    case (f ow g, a) if a(f).talkative => f.e
    case ((Stop() in _) ow g, a) => g.e
  }
  val ConstProp = Opt("constant-propogation") {
    case (g < x <| ((y : Argument) in _), a) => g.e.subst(y, x)
    case (((y : Constant) in _) > x > g, a) => g.e.subst(y, x)
    case (((y : Argument) in ctx) > x > g, a) if a(y in ctx).immediatePublish => g.subst(y, x) // FIXME: This may not be triggering in every case that it should.
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(Pars(es, ctx) < x <| g), a) if es.exists(!_.freevars.contains(x)) => {
      val (f, h) = es.partition(_.freevars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (Pars(f) < x <| g) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }
  
  val LimitElim = Opt("limit-elim") {
    case (LimitAt(f), a) if a(f).publishesAtMost(1) && a(f).effectFree => f
  }
  val LimitCompChoice = Opt("limit-compiler-choice") {
    case (LimitAt(Pars(fs, ctx)), a) if fs.size > 1 && fs.exists(f => a(f in ctx).immediatePublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f in ctx).immediatePublish)
      Limit(f1)
    }    
  }
  
  val costThreshold = 15
  
  val InlineDef = OptFull("inline-def") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case CallAt((f: BoundVar) in ctx, args, _, _) => ctx(f) match {
        case Bindings.DefBound(dctx, _, d) => {
          def cost = Analysis.cost(d.body)
          val bodyfree = d.body.freevars
          def recursive = bodyfree.contains(d.name)
          def ctxsCompat = {
            def isRelevant(b: Bindings.Binding) = bodyfree.contains(b.variable) && b.variable != d.name
            val ctxTrimed = ctx.bindings.filter(isRelevant)
            val dctxTrimed = dctx.bindings.filter(isRelevant)
            ctxTrimed == dctxTrimed
          }
          //if (cost > costThreshold) 
          //  println(s"WARNING: Not inlining ${d.name} because of cost ${cost}")
          //if( !ctxsCompat ) 
          //println(s"${d.name} compatibily:\n$ctxTrimed\n$dctxTrimed")
          if ( recursive || Analysis.cost(d.body) > costThreshold || !ctxsCompat)
            None // No inlining of recursive functions or large functions.
          else
            Some(d.body.substAll(((d.formals: List[Argument]) zip args).toMap))
        }
        case _ => None
      }
      case _ => None
    }
  }
  
  val DefElim = Opt("def-elim") {
    case (DeclareDefsAt(defs, ctx, b), a) if (b.freevars & defs.map(_.name).toSet).isEmpty => b
  }
  
  def isClosureBinding(b: Bindings.Binding) = {
    import Bindings._
    b match {
      case DefBound(_,_,_) | RecursiveDefBound(_,_,_) => true
      case _ => false
    } 
  }
  
  val AccessorElim = Opt("accessor-elim") {
    case (CallAt(Constant(GetField) in _, List(Constant(r : OrcRecord), Constant(f: Field)), ctx, _), a) if r.entries.contains(f.field) => 
      Constant(r.getField(f))
    case (CallAt(Constant(ProjectClosure) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("apply") => 
      Constant(r.getField(Field("apply")).get)
    case (CallAt(Constant(ProjectClosure) in _, List(v : BoundVar), _, ctx), a) if isClosureBinding(ctx(v)) => 
      v
    case (CallAt(Constant(ProjectUnapply) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("unapply") => 
      Constant(r.getField(Field("unapply")).get)
  }
  
  val basicOpts = List(AccessorElim, DefElim, LateBindReorder, LiftUnrelated, LimitCompChoice, LimitElim, ConstProp, StopEquiv, LateBindElim, ParElim, OWElim, SeqExp, SeqElim, SeqElimVar)
  val secondOpts = List(InlineDef)
  
  // This optimization makes mandelbrot slightly slower. I'm not sure why. I think it will be a useful optimization in some cases anyway. It may be that when instruction dispatch is faster removing parallelism will be more effective.
  val flatteningOpts = List(LateBindElimFlatten)
}

