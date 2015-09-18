//
// Optimizer.scala -- Scala Optimizer
// Project OrcScala
//
// $Id$
//
// Created by amp on Sept 16, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.compile.Logger
import orc.values.OrcRecord
import orc.ast.orctimizer.named._
import Bindings.{DefBound, RecursiveDefBound, SeqBound}
import orc.values.Field
import orc.lib.builtin.structured.TupleConstructor
import orc.lib.builtin.structured.TupleArityChecker
import orc.compile.CompilerOptions
import orc.types
import orc.values.sites.Delay
import orc.values.sites.Effects
import orc.values.sites.Site


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

// TODO: Implement compile time evaluation of select sites.

/**
  *
  * @author amp
  */
case class Optimizer(co: CompilerOptions) {
  def apply(e : Expression, analysis : ExpressionAnalysisProvider[Expression]) : Expression = {
    val trans = new ContextualTransform.Pre {
      override def onExpressionCtx = {
        case (e: WithContext[Expression]) => {
          val e1 = opts.foldLeft(e)((e, opt) => {
            opt(e, analysis) match {
              case None => e
              case Some(e2) =>
                if (e.e != e2) {
                  Logger.fine(s"${opt.name}: ${e.e.toString.replace("\n", " ").take(80)} ==> ${e2.toString.replace("\n", " ").take(80)}")
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

  import Optimizer._

  val flattenThreshold = co.options.optimizationFlags("orct:future-flatten-threshold").asInt(5)

  val FutureElimFlatten = Opt("future-elim-flatten") {
    case (FutureAt(g) > x > f, a) if a(f).forces(x) <= ForceType.Eventually && (a(g).publications only 1) && Analysis.cost(g) <= flattenThreshold => g > x > f
  }
  val FutureElim = Opt("future-elim") {
    case (FutureAt(g) > x > f, a) if a(f).forces(x) == ForceType.Immediately(true) && a(g).publications <= 1 => g > x > f
    case (FutureAt(g) > x > f, a) if a(g).nonBlockingPublish && (a(g).publications only 1) => g > x > f
    case (FutureAt(g) > x > f, a) if !(a(f).freeVars contains x) => f || (g >> Stop()) 
    case (FutureAt(g) > x > (Stop() in _), a) => g > x > Stop()
  }
  val FutureForceElim = OptFull("future-force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case FutureAt(ForceAt((x: BoundVar) in ctx)) => ctx(x) match {
        case Bindings.SeqBound(ctx, Future(_) > _ > _) => Some(x)
        case _ => None
      }
      case _ => None
    }
  }
  val ForceElim =  OptFull("force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case ForceAt(c@Constant(_) in ctx) => Some(c)
      case ForceAt((x: BoundVar) in ctx) => ctx(x) match {
        case Bindings.SeqBound(ctx, Call(Constant(_: Site), _, _) > _ > _) => Some(x)
        case _ => None
      }
      case _ => None
    }
  }
  Opt("force-elim") {
    case (ForceAt(c@Constant(_) in ctx), a) => c
    case (ForceAt(c@Constant(_) in ctx), a) => c
  }

  val StopEquiv = Opt("stop-equiv") {
    case (f, a) if f != Stop() && 
      (a(f).publications only 0) && 
      a(f).effects == Effects.None && 
      a(f).timeToHalt == Delay.NonBlocking => 
        Stop()
  }
  val SeqElim = Opt("seq-elim") {
    case (f > x > g, a) if a(f).silent => f
    case (f > x > g, a) if a(f).effectFree && a(f).nonBlockingPublish && 
      (a(f).publications only 1) && a(f).nonBlockingHalt && !a(g).freeVars.contains(x) => g
  }
  val SeqElimVar = OptFull("seq-elim-var") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > y if x == y.e => Some(f)
      case ((x: Var) in _) > y > f if f.forces(y) == ForceType.Immediately(true) => Some(f.subst(x, y))
      case ForceAt((x: Var) in _) > y > f if f.forces(x) == ForceType.Immediately(true) && !(a(f).freeVars contains y) => Some(f)
      case _ => None
    }
  }

  /*
  val SeqRHSInline= OptFull("seq-rhs-inline") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case f > x > g if g strictOn x => 
      case _ => None
    }
  } 
  */
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
  
  val SeqReassoc = Opt("seq-reassoc") {
    case (Seqs(ss, en), a) if ss.size > 1 => {
      Seqs(ss, en) 
    }
  }
  val DefSeqNorm = Opt("def-seq-norm") {
    case (DeclareDefsAt(defs, ctx, b) > x > e, a) if (a(e).freeVars & defs.map(_.name).toSet).isEmpty  => {
       DeclareDefs(defs, b > x > e)
    }
  }
 
  val ParElim = OptSimple("par-elim") {
    case (Stop() in _) || g => g.e
    case f || (Stop() in _) => f.e
  }
  val ConstProp = Opt("constant-propogation") {
    case (((y : Constant) in _) > x > g, a) => g.e.subst(y, x)
    case (((y : Argument) in ctx) > x > g, a) if a(y in ctx).nonBlockingPublish => g.subst(y, x) 
    // FIXME: This may not be triggering in every case that it should.
  }

  val LiftUnrelated = Opt("lift-unrelated") {
    case (e@(g > x > Pars(es, ctx)), a) if a(g).nonBlockingPublish && 
            es.exists(e => !a(e in ctx).freeVars.contains(x)) => {
      val (f, h) = es.partition(e => a(e in ctx).freeVars.contains(x))
      (f.isEmpty, h.isEmpty) match {
        case (false, false) => (g > x > Pars(f)) || Pars(h)
        case (true, false) => (g >> Stop()) || Pars(h)
        case (false, true) => e
      }
    }
  }
  
  val LimitElim = Opt("limit-elim") {
    case (LimitAt(f), a) if a(f).publications <= 1 && a(f).effects <= Effects.BeforePub => f
  }
  val LimitCompChoice = Opt("limit-compiler-choice") {
    case (LimitAt(Pars(fs, ctx)), a) if fs.size > 1 && fs.exists(f => a(f in ctx).nonBlockingPublish) => {
      // This could even be smarter and pick the "best" or "fastest" expression.
      val Some(f1) = fs.find(f => a(f in ctx).nonBlockingPublish)
      Limit(f1)
    }    
  }
  
  val SiteForceElim = OptFull("site-force-elim") { (e, a) =>
    import a.ImplicitResults._
    e match {
      case call@CallAt(target@Constant(_ : Site) in _, args, targs, ctx) => {
        val newargs = for (a <- args) yield {
          a match {
            case x: BoundVar => ctx(x) match {
              case Bindings.SeqBound(_, Force(v) > _ > _) => v
              case _ => x
            }
            case _ => a
          }
        }
        Some(Call(target, newargs, targs))
      }
      case _ => None
    }
  }

  
  val inlineCostThreshold = co.options.optimizationFlags("orct:inline-threshold").asInt(15)
  
  val InlineDef = OptFull("inline-def") { (e, a) =>
    import a.ImplicitResults._
    
    e match {
      case CallAt((f: BoundVar) in ctx, args, targs, _) => ctx(f) match {
        case Bindings.DefBound(dctx, _, d) => {
          def cost = Analysis.cost(d.body)
          val bodyfree = (d.body in dctx).freeVars
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
          if ( recursive || Analysis.cost(d.body) > inlineCostThreshold || !ctxsCompat)
            None // No inlining of recursive functions or large functions.
          else {
            val bodyWithValArgs = d.body.substAll(((d.formals: List[Argument]) zip args).toMap)
            val typeSubst = targs match {
              case Some(as) => (d.typeformals:List[Typevar]) zip as
              case None => (d.typeformals:List[Typevar]) map { (t) => (t, Bot()) }
            }
            val newBody = bodyWithValArgs.substAllTypes(typeSubst.toMap)
            Some(newBody)
          }
        }
        case _ => None
      }
      case _ => None
    }
  }
  
  val DefElim = Opt("def-elim") {
    case (DeclareDefsAt(defs, ctx, b), a) if (a(b).freeVars & defs.map(_.name).toSet).isEmpty => b
  }
  
  def isClosureBinding(b: Bindings.Binding) = {
    import Bindings._
    b match {
      case DefBound(_,_,_) | RecursiveDefBound(_,_,_) => true
      case _ => false
    } 
  }
  
  /*
  val AccessorElim = Opt("accessor-elim") {
    case (CallAt(Constant(GetField) in _, List(Constant(r : OrcRecord), Constant(f: Field)), ctx, _), a) if r.entries.contains(f.field) => 
      Constant(r.getField(f))
    case (CallAt(Constant(ProjectClosure) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("apply") => 
      Constant(r.getField(Field("apply")))
    case (CallAt(Constant(ProjectClosure) in _, List(v : BoundVar), _, ctx), a) if isClosureBinding(ctx(v)) => 
      v
    case (CallAt(Constant(ProjectUnapply) in _, List(Constant(r : OrcRecord)), ctx, _), a) if r.entries.contains("unapply") => 
      Constant(r.getField(Field("unapply")))
  }
  
  val TupleElim = OptFull("tuple-elim") { (e, a) =>
    import a.ImplicitResults._, Bindings._
    e match {
      case CallAt(Constant(GetElem) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) if (v in ctx).immediatePublish => 
        val i = bi.intValue
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size && (args(i) in tctx).immediatePublish => Some(args(i))
          case _ => None
        }
      case CallAt(Constant(GetElem) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) > x > e if (v in ctx).immediatePublish && !e.freevars.contains(x) => 
        val i = bi.intValue
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) if i < args.size => Some(e)
          case _ => None
        }
      case CallAt(Constant(TupleArityChecker) in _, List(v: BoundVar, Constant(bi: BigInt)), _, ctx) if (v in ctx).immediatePublish => 
        val i = bi.intValue
        ctx(v) match {
          case SeqBound(tctx, Call(Constant(TupleConstructor), args, _) > `v` > _) if i == args.size => Some(v)
          case _ => None
        }
        /*
      case CallAt(Constant(TupleConstructor) in _, args, _, ctx) 
              if args.size > 0 && args.forall(_.isInstanceOf[BoundVar]) =>
        def lookup(v: BoundVar) = ctx(v) match {
          case SeqBound(tctx, Call(Constant(GetElem), List(v: BoundVar, Constant(bi: BigInt)), _) > `v` > _) => 
            Some((v, bi))
          case _ => None
        }
        
        val vars = args.flatMap { 
          case v: BoundVar => lookup(v)
          case _ => None
        }

        vars.groupBy(_._1).toSeq match {
          case Seq((tv, inds)) => 
            e.typeOf(tv).runtimeType match {
              case types.TupleType(ts) if inds == (0 until ts.size) => Some(tv)
              case _ => None
            }
        }
        */
      case _ => None
    }
  }
  */

  val allOpts = List(
      SeqReassoc,
      DefSeqNorm, DefElim, 
      LiftUnrelated, 
      FutureElimFlatten, FutureElim, 
      FutureForceElim, ForceElim,
      SiteForceElim,
      LimitCompChoice, LimitElim, ConstProp, 
      StopEquiv, ParElim,
      SeqExp, SeqElim, SeqElimVar,
      InlineDef
      )

  val opts = allOpts.filter{ o =>
    co.options.optimizationFlags(s"orct:${o.name}").asBool()
  }
}

object Optimizer {
  import WithContext._
  
  //Logger.logAllToStderr()
  
  object Pars {
    private def pars(p: Expression): List[Expression] = {
      p match {
        case f || g => pars(f) ++ pars(g)
        case e => List(e)
      }
    }
    def unapply(e: Expression): Option[List[Expression]] = Some(pars(e))
    def unapply(e: WithContext[Expression]): Option[(List[Expression], TransformContext)] = {
      Some(pars(e.e), e.ctx)
    }
    
    def apply(l : Traversable[Expression]) = l.reduce(_ || _)
  }
  
  /**
   * Match a sequence of expressions in the form: e1 >x1> ... >xn-1> en (ignoring association)
   */
  object Seqs {
    /*private def latebinds(p: Expression): (Expression, List[(BoundVar, Expression)]) = {
      p match {
        case f < x <| g => {
          val (e1, lbs) = latebinds(f)
          (e1, lbs :+ (x, g))
        }
        case e => (e, Nil)
      }
    }*/
    private def seqsAt(p: WithContext[Expression]): (List[(WithContext[Expression], BoundVar)], WithContext[Expression]) = {
      p match {
        case f > x > g => {
          val (fss, fn) = seqsAt(f) // f1 >x1> ... >xn-1> fn
          val (gss, gn) = seqsAt(g) // g1 >y1> ... >yn-1> gn 
          // TODO: Add an assert here to check that no variables are shadowed. This should never happen because of how variables are handled and allocated.
          // f1 >x1> ... >xn-1> fn >x> g1 >y1> ... >yn-1> gn
          (fss ::: (fn, x) :: gss, gn)
        }
        case e => (Nil, e)
      }
    }

    def unapply(e: WithContext[Expression]): Option[(List[(WithContext[Expression], BoundVar)], WithContext[Expression])] = {
      Some(seqsAt(e))
    }
    
    def apply(ss: List[(WithContext[Expression], BoundVar)], en: Expression) : Expression = {
      ss match {
        case Nil => en
        case (e, x) :: sst => e > x > apply(sst, en)
      }
    }
  }
   
  // TODO: Might need a sequence version of this for rearranging sequences of futures.
  /*
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
  */
}

