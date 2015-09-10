//
// Analysis.scala -- Scala class/trait/object Analysis
// Project OrcScala
//
// $Id$
//
// Created by amp on Apr 30, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import scala.collection.mutable
import orc.ast.orctimizer.named.in
import orc.values.Field
import orc.values.sites.Site

trait Time

/**
 * The analysis results of an expression. The results refer to this object used an 
 * expression. So for variables publication is in terms of what happens when you
 * access the variable and publish the value you get (or don't get).
 */
trait AnalysisResults {
  /**
   * Does this expression halt immediately?
   */
  def timeToHalt: Time 
  
  /**
   * Does this expression publish immediately?
   */
  def timeToPublish: Time 
  
  /**
   * This expression only has side-effects before this time.
   */
  def timeToLastEffect: Time
 
  /**
   * How many times will this expression publish?
   * The pair is minimum and maximum where None represents
   * no maximum.
   */
  def publications: Range

  /**
   * Does this expression force the variable <code>x</code> before performing any visible 
   * action other than halting?
   * Specifically is <code>x</code> forced before the expression has side effects or publishes.
   */
  def forces(x : Var) = forceTimes(x)
  def forceTimes : Map[Var, Time]
}

case class AnalysisResultsConcrete(
  timeToHalt: Time, 
  timeToPublish: Time, 
  timeToLastEffect: Time, 
  publications: Range, 
  forceTimes : Map[Var, Time]
    ) extends AnalysisResults {
  // TODO: Add validity checks to catch unreasonable combinations of values
}

sealed trait ExpressionAnalysisProvider[E <: Expression] {
  outer =>
  def apply(e: E)(implicit ctx: TransformContext) : AnalysisResults
  def apply(e : WithContext[E]) : AnalysisResults = this(e.e)(e.ctx)
  def get(e: E)(implicit ctx: TransformContext) : Option[AnalysisResults]
  
  object ImplicitResults {
    import scala.language.implicitConversions
    @inline implicit def expressionWithResults(e : E)(implicit ctx: TransformContext) : AnalysisResults = apply(e) 
    @inline implicit def expressionCtxWithResults(e : WithContext[E]) : AnalysisResults = apply(e.e)(e.ctx) 
  }
  
  //def toMap : collection.Map[(TransformContext, E), AnalysisResults]
  
  /*
  def withDefault : ExpressionAnalysisProvider[E] = {
    new ExpressionAnalysisProvider[E] {
      def apply(e: E)(implicit ctx: TransformContext) : AnalysisResults = get(e).getOrElse(AnalysisResultsConcrete(false, false, Range(0, None), Set(), Set(), false))
      def get(e: E)(implicit ctx: TransformContext) : Option[AnalysisResults] = outer.get(e)
    }
  }
  */
}

/**
 * A cache for storing all the results of a bunch of expressions.
 */
class ExpressionAnalyzer extends ExpressionAnalysisProvider[Expression] {
  val cache = mutable.Map[(TransformContext, Expression), AnalysisResults]()
  def apply(e : Expression)(implicit ctx: TransformContext) = {
    cache.get((ctx, e)) match {
      case Some(r) => {
        //println(s"Cache hit: ${e.toString.take(40).replace('\n', ' ')} ${ctx.toString.take(40).replace('\n', ' ')}")
        r
      }
      case None => {
        //println(s"Cache miss: ${e.toString.take(40).replace('\n', ' ')} ${ctx.toString.take(40).replace('\n', ' ')}")
        val r = analyze(WithContext(e, ctx))
        cache += (ctx, e) -> r 
        /*if( cache.size % 1000 == 0 ) {
          println(s"Cache is ${cache.size}")
        }*/
        r
      }
    }
  }
  def get(e : Expression)(implicit ctx: TransformContext) = Some(this(e))
  
  // TODO: Recursive functions could be handled by thinking of these functions as recursive relations on the analysis result.
  // These recursive relations could either be solve in place as we go or reified and then solved using any technique.

  // TODO: The analysis I am doing is flow-sensative and outward-context-sensative in that functions are 
  //       analyzed for each call site and that result used at that site. However no information flow 
  //       inward from the call site to allow optimization inside the function body. A conscious decision 
  //       should be made as to what sensativities we want.
   
  
  // TODO: All these analyses are really flow analyses of one form or another. This should use a consistant 
  //       framework for them. However because we have no goto all flow control is through functions which
  //       means flow analyses MUST occur across function boundries.
  
  def analyze(e : WithContext[Expression]) : AnalysisResults = {
    ???
    //AnalysisResultsConcrete(immediateHalt(e), immediatePublish(e), publications(e), strictOn(e), forces(e), effectFree(e))
  }
  
  /*
  def immediateHalt(e : WithContext[Expression]): Boolean = {
    import ImplicitResults._
    e match {
      case Stop() in _ => true
      case f || g => f.immediateHalt && g.immediateHalt
      case f ow g => (f.immediateHalt && g.immediateHalt) || 
                     (f.immediateHalt && f.publications > 0)
      case f > x > g => (f.immediateHalt && g.immediateHalt) || 
                        (f.immediateHalt && f.silent)
      case f < x <| g => (f.immediateHalt && g.immediateHalt)
      case LimitAt(f) => f.immediateHalt || f.immediatePublish
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        (target match {
          case Constant(s : Site) => s.immediateHalt || args.exists(a => a.immediateHalt && a.silent)
          case v : BoundVar => v.immediateHalt && v.silent
          case _ => false
        }) && args.forall(a => a.immediatePublish || a.immediateHalt)
      }
      case DeclareDefsAt(defs, defsctx, body) => {
        val v = body.immediateHalt
        /*if(defs.exists(_.name.optionalVariableName == Some("toattr")) && !v) {
          println("Here: " + (body))
        }*/
        v
      }
      case DeclareTypeAt(_, _, b) => b.immediateHalt
      case HasType(b, _) in ctx => this(b)(ctx).immediateHalt
      case Constant(_) in _ => true
      case (v : BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _) 
           | Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => true 
        case b : Bindings.LateBound => {
          b.valueExpr.immediateHalt
        }
        case _ => false
        // FIXME: There should be cases for Bindings.DefBound(_, _, _) | Bindings.RecursiveDefBound(_, _, _)
      }
      case _ => false
    }
  }
  def immediatePublish(e : WithContext[Expression]): Boolean = {
    import ImplicitResults._
    e match {
      case Stop() in _ => false
      case f || g => f.immediatePublish || g.immediatePublish
      case f ow g => (f.immediatePublish) || 
                     (f.immediateHalt && g.immediatePublish)
      case f > x > g => (f.immediatePublish && g.immediatePublish)
      case f < x <| g => f.immediatePublish 
      case LimitAt(f) => f.immediatePublish
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx

        target match {
          case Constant(s: Site) => s.immediatePublish && args.forall(_.immediatePublish)
          case v: BoundVar => false
          case _ => false
        }
      }
      case DeclareDefsAt(defs, _, body) => {
        body.immediatePublish
      }
      case DeclareTypeAt(_, _, b) => b.immediatePublish
      case HasType(b, _) in ctx => this(b)(ctx).immediatePublish
      case Constant(_) in _ => true
      case (v : BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _) 
           | Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => true
        case b : Bindings.LateBound => {
          b.valueExpr.immediatePublish
        }
        case _ => false
        // FIXME: There should be cases for Bindings.DefBound(_, _, _) | Bindings.RecursiveDefBound(_, _, _)
      }
      case _ => false
    }
  }
  */
 
  def publications(e : WithContext[Expression]): Range = {
    import ImplicitResults._
    e match {
      case Stop() in _ => Range(0,0)
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        (target match {
        case Constant(s : Site) => {
          if( args.forall(a => a.publications > 0) )
            Range(s.publications)
          else if( args.exists(a => a.publications only 0) )
            Range(0, 0)
          else
            Range(s.publications).mayHalt
        }
        case _ => Range(0, None)
      })
      }
      case LimitAt(f) => f.publications.limitTo(1)
      case Force(x: BoundVar) in ctx => ctx(x) match {
        case Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => Range(1,1)
        case _ => Range(0, None)
      }
      case Future(f) in _ => Range(1,1)
      case f || g => f.publications + g.publications
      case f ConcatAt g => f.publications + g.publications
      case (f > x > g) in ctx => (f in ctx).publications * (g in ctx).publications
      case DeclareDefsAt(defs, defsctx, body) => {
        body.publications
      }
      case DeclareTypeAt(_, _, b) => b.publications
      case HasType(b, _) in ctx => this(b)(ctx).publications
      case Constant(_) in _ => Range(1,1)
      case (v : BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _) 
           | Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => Range(1,1)
        case _ => ???
      }
      case _ => Range(0, None)
    }
  }

  /*
  def strictOn(e : WithContext[Expression]): Set[Var] =  {
    import ImplicitResults._
    e match {
      case Stop() in _ => Set()
      case CallAt(target in _, args, _, ctx) => (target match {
        case Constant(s : Site) => {
          val vars = (args collect { case v : Var => v })
          val mayStops = vars filter { v => !((v in ctx).publications > 0) }
          mayStops match {
            case List() => vars.toSet
            case List(v) => Set(v)
            case _ => Set()
          }
        }
        case v : BoundVar => {
          val callstr = ctx(v) match {
            case Bindings.DefBound(ctx, _, d) => d in ctx match {
              case DefAt(name in _, formals, body, _, _, _, _) => {
                assert(name == v)
                val m = (formals zip args).toMap[Var, Argument]
                body.strictOnSet.collect(m).collect{ case v : Var => v }
              }
            }
            case _ => Set[Var]()
          }
          callstr + v
        }
        case v : Var => Set(v)
        case _ => Set()
      })
      case f || g => f.strictOnSet & g.strictOnSet
      case f ow g => f.strictOnSet
      case f > x > g => f.strictOnSet
      case f > x > g if f.effectFree && f.immediatePublish => f.strictOnSet ++ (g.strictOnSet - x)
      case f < x <| g if f.strictOn(x) => g.strictOnSet 
      case f < x <| g => f.strictOnSet & g.strictOnSet
      case LimitAt(f) => f.strictOnSet
      case DeclareDefsAt(defs, defsctx, body) => {
        body.strictOnSet
      }
      case DeclareTypeAt(_, _, b) => b.strictOnSet
      case HasType(b, _) in ctx => (b in ctx).strictOnSet
      case Constant(_) in _ => Set()
      case (v : BoundVar) in ctx => {
        ctx(v) match {
          /*case Bindings.DefBound(ctx2, _, d) => d in ctx match {
              case DefAt(name in _, formals, body, _, _, _, _) => {
                assert(name == v)
                ((body.freevars -- formals) + v).collect{case v : Var => v}
              }
            }*/
          case _ => Set(v)
        }
      }
      case _ => Set()
    }
  }
  def forces(e : WithContext[Expression]): Set[Var] =  {
    import ImplicitResults._
    e match {
      case Stop() in _ => Set()
      case CallAt(_, _, _, _) => strictOn(e)
      case f || g => f.forcesSet & g.forcesSet
      case f ow g => f.forcesSet
      case f > x > g if f.effectFree => f.forcesSet ++ (g.forcesSet - x) // Adding f.publishesAtLeast(1) means that this expression cannot halt without forcing.
      case f > x > g => f.forcesSet
      case f < x <| g if f.forces(x) => g.forcesSet ++ (f.forcesSet - x)
      case f < x <| g => f.forcesSet & g.forcesSet 
      case LimitAt(f) => f.forcesSet
      case DeclareDefsAt(defs, defsctx, body) => {
        body.forcesSet
      }
      case DeclareTypeAt(_, _, b) => b.forcesSet
      case HasType(b, _) in ctx => (b in ctx).forcesSet
      case (v : BoundVar) in _ => strictOn(e)
      case _ => Set()
    }
  }
  
  def effectFree(e : WithContext[Expression]): Boolean = {
    import ImplicitResults._
    e match {
      case Stop() in _ => true
      case CallAt(target in _, args, _, _) => (target match {
        case Constant(s : Site) => s.effectFree
        case _ => false
      })
      case f || g => f.effectFree && g.effectFree
      case f ow g => (f.effectFree && g.effectFree) ||
                     (f.talkative && f.effectFree)
      case f > x > g => (f.effectFree && g.effectFree) ||
                        (f.silent && f.effectFree)
      case f < x <| g => f.effectFree && g.effectFree
      case LimitAt(f) => f.effectFree
      case DeclareDefsAt(defs, defsctx, body) => {
        body.effectFree
      }
      case DeclareTypeAt(_, _, b) => b.effectFree
      case HasType(b, _) in ctx => this(b)(ctx).effectFree
      case Constant(_) in _ => true
      case (v : BoundVar) in _ => true
      case _ => false
    }
  }
  */
}
