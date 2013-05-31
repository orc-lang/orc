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


trait Optimization extends ((Expression, ExpressionAnalysisProvider[Expression], OptimizationContext) => Option[Expression]) {
  //def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Expression = apply((e, analysis, ctx))
  def name : String
}

case class Opt(name : String)(f : PartialFunction[(Expression, ExpressionAnalysisProvider[Expression]), Expression]) extends Optimization {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Option[Expression] = f.lift((e, analysis))
}
case class OptSimple(name : String)(f : PartialFunction[Expression, Expression]) extends Optimization {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Option[Expression] = f.lift(e)
}
case class OptFull(name : String)(f : (Expression, ExpressionAnalysisProvider[Expression], OptimizationContext) => Option[Expression]) extends Optimization {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx: OptimizationContext) : Option[Expression] = f(e, analysis, ctx)
}

case class OptimizationContext(val defs : Map[BoundVar, Def] = Map())  {
  def apply(e : BoundVar) : Option[Def] = defs.get(e)
  def toMap = defs
  
  def this() = this(Map())
  
  def extend(bs : Iterable[Def]) : OptimizationContext = {
    new OptimizationContext(defs ++ bs.map(b => (b.name, b)))
  }
  def ++(bs : Iterable[Def]) = extend(bs)
  
  def +(b : Def) : OptimizationContext = {
    new OptimizationContext(defs + ((b.name, b)))
  }
}

/**
  *
  * @author amp
  */
class Optimizer(opts : Seq[Optimization]) {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression], ctx : OptimizationContext = OptimizationContext()) : Expression = {
    val e1 = e match {
      case f || g => apply(f, analysis, ctx) || apply(g, analysis, ctx)
      case f ow g => apply(f, analysis, ctx) ow apply(g, analysis, ctx)
      case f > x > g => apply(f, analysis, ctx) > x > apply(g, analysis, ctx)
      case f < x <| g => apply(f, analysis, ctx) < x <| apply(g, analysis, ctx)
      case Limit(f) => Limit(apply(f, analysis, ctx))
      case DeclareDefs(defs, body) => {
        val ctx1 = ctx ++ defs
        // Note: the optimization of the bodies is NOT done with the defs in context. That would result in unbounded inlining.
        DeclareDefs(defs.map(d => d.copy(body = apply(d.body, analysis, ctx))), apply(body, analysis, ctx1))
      }
      case d@DeclareType(_, _, b) => d.copy(body = apply(d.body, analysis, ctx))
      case d@HasType(b, _) => d.copy(body = apply(d.body, analysis, ctx))
      case _ => e
    }
    opts.foldLeft(e1)((e, opt) => {
      opt(e, analysis, ctx) match {
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
  object Pars {
    private def pars(p: Expression): Set[Expression] = {
      p match {
        case f || g => pars(f) ++ pars(g)
        case e => Set(e)
      }
    }
    def unapply(e: Expression): Option[Set[Expression]] = Some(pars(e))
    
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
    def unapply(e: Expression): Option[(Expression, List[(BoundVar, Expression)])] = Some(latebinds(e))
    
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
  }
  
  val Test = Opt("Test") {
    case (e@IndependantLateBinds(f, lbs), a) => println(s"Test: $f \n<<|\n $lbs\n........"); e
  }

  val LateBindReorder = Opt("late-bind-reorder") {
    case (IndependantLateBinds(f, lbs), a) if lbs.exists(p => a(f).strictOn(p._1)) => {
      val lbs1 = lbs.sortBy(p => !a(f).strictOn(p._1))
      LateBinds(f, lbs1)
    }
  }
  val LateBindElim = Opt("late-bind-elim") {
    case (f < x <| g, a) if a(f).strictOn(x) && a(g).publishesAtMost(1) => g > x > f
    case (f < x <| g, a) if a(g).immediatePublish && a(g).publishesAtMost(1) => g > x > f
    case (Stop() < x <| g, a) => g > x > Stop()
  }
  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f != Stop() && a(f).silent && a(f).effectFree && a(f).immediateHalt => Stop()
  }
  val SeqElim = Opt("seq-elim") {
    case (f > x > g, a) if a(f).silent => f
  }
  val SeqExp = Opt("seq-expansion") {
    case (e@(Pars(fs) > x > g), a) if fs.exists(f => a(f).silent) => {
      // This doesn't really eliminate any code and I cannot think of a case where 
      val (sil, nsil) = fs.partition(f => a(f).silent)
      (sil.isEmpty, nsil.isEmpty) match {
        case (false, false) => (Pars(nsil) > x > g) || Pars(sil)
        case (false, true) => Pars(sil)
        case (true, false) => e
      }
    }
    case (e@(Pars(fs) > x > g), a) if fs.exists(f => f.isInstanceOf[Constant]) => {
      val cs = fs.collect{ case c : Constant => c }
      val es = fs.filter(f => !f.isInstanceOf[Constant])
      (cs.isEmpty, es.isEmpty) match {
        case (false, false) => (Pars(es) > x > g) || Pars(cs.map(g.subst(_, x)))
        case (false, true) => Pars(cs.map(g.subst(_, x)))
        case (true, false) => e
      }
    }
  }
  val SeqElimVar = Opt("seq-elim-var") {
    case (f > x > y, a) if x == y => f
  }
  val ParElim = OptSimple("par-elim") {
    case Stop() || g => g
    case f || Stop() => f
  }
  val OWElim = Opt("otherwise-elim") {
    case (f ow g, a) if a(f).talkative => f
    case (Stop() ow g, a) => g
  }
  val ConstProp = Opt("constant-propogation") {
    case (g < x <| (y : Argument), a) => g.subst(y, x)
    case ((y : Constant) > x > g, a) => g.subst(y, x)
    case ((y : Argument) > x > g, a) if a(y).immediatePublish => g.subst(y, x)
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(Pars(es) < x <| g), a) if es.exists(!_.freevars.contains(x)) => {
      val (f, h) = es.partition(_.freevars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (Pars(f) < x <| g) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }
  
  val LimitElim = Opt("limit-elim") {
    case (Limit(f), a) if a(f).publishesAtMost(1) && a(f).effectFree => f
  }
  val LimitCompChoice = Opt("limit-compiler-choice") {
    case (Limit(Pars(fs)), a) if fs.size > 1 && fs.exists(f => a(f).immediatePublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f).immediatePublish)
      Limit(f1)
    }    
  }
  
  val InlineDef = OptFull("inline-def") { (e, a, ctx) =>
    e match {
      case Call(f : BoundVar, args, _) if ctx(f).isDefined => {
        val Some(d) = ctx(f)
        Some(d.body.substAll(((d.formals:List[Argument]) zip args).toMap))
      } 
      case _ => None
    }
  }
  
  val DefElim = Opt("def-elim") {
    case (DeclareDefs(defs, b), a) if (b.freevars & defs.map(_.name).toSet).isEmpty => b
  }
  
  val defaultOptimizer = new Optimizer(List(DefElim, LateBindReorder, LiftUnrelated, LimitCompChoice, LimitElim, ConstProp, StopEquiv, LateBindElim, ParElim, OWElim, SeqExp, SeqElim, SeqElimVar))
}

