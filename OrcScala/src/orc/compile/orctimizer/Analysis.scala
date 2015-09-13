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
import orc.values.sites.Delay
import orc.values.sites.Effects

sealed trait ForceType {
  val haltsWith = false
  def max(o: ForceType): ForceType
  def min(o: ForceType): ForceType
  
  def nonHalting: ForceType = this
  def delayBy(d: Delay): ForceType = d match {
    case Delay.NonBlocking => this
    case Delay.Blocking => this min ForceType.Eventually
    case Delay.Forever => this min ForceType.Never
  }
}
object ForceType {
  case class Immediately(override val haltsWith: Boolean) extends ForceType {
    def max(o: ForceType): ForceType = o match {
      case Immediately(_) => Immediately(haltsWith && o.haltsWith)
      case _ => o
    }
    def min(o: ForceType): ForceType = Immediately(haltsWith || o.haltsWith)
    
    override def nonHalting = Immediately(false)
  }
  case object Eventually extends ForceType {
    def max(o: ForceType): ForceType = o match {
      case Never => o
      case _ => this
    }
    def min(o: ForceType): ForceType = o match {
      case Immediately(_) => o
      case _ => this
    }
  }
  case object Never extends ForceType {
    def max(o: ForceType): ForceType = this
    def min(o: ForceType): ForceType = o
  }
  
  def maxMaps[T](a: Map[T, ForceType], b: Map[T, ForceType]): Map[T, ForceType] = {
    a ++ b.map{ case (k, v) => k -> (v max a.getOrElse(k, Immediately(false))) }
  }
  def minMaps[T](a: Map[T, ForceType], b: Map[T, ForceType]): Map[T, ForceType] = {
    a ++ b.map{ case (k, v) => k -> (v min a.getOrElse(k, Never)) }
  }
}

/**
 * The analysis results of an expression. The results refer to this object used an 
 * expression. So for variables publication is in terms of what happens when you
 * access the variable and publish the value you get (or don't get).
 */
trait AnalysisResults {
  /**
   * Does this expression halt immediately?
   */
  def timeToHalt: Delay 
  
  /**
   * Does this expression publish immediately?
   */
  def timeToPublish: Delay 
  
  /**
   * This expression only has side-effects before this time.
   */
  def effects: Effects
 
  /**
   * How many times will this expression publish?
   */
  def publications: Range

  /**
   * Does this expression force the variable <code>x</code> before performing any visible 
   * action other than halting?
   * Specifically is <code>x</code> forced before the expression has side effects or publishes.
   */
  def forces(x : Var) = forceTypes(x)
  def forceTypes : Map[Var, ForceType]
  
  /**
   * Free variables in the expression.
   */
  def freeVars: Set[Var]
}

case class AnalysisResultsConcrete(
  timeToHalt: Delay, 
  timeToPublish: Delay, 
  effects: Effects, 
  publications: Range, 
  forceTypes : Map[Var, ForceType],
  freeVars: Set[Var]
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
    @inline 
    implicit def expressionWithResults(e : E)(implicit ctx: TransformContext) : AnalysisResults = apply(e) 
    @inline 
    implicit def expressionCtxWithResults(e : WithContext[E]) : AnalysisResults = apply(e.e)(e.ctx) 
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
    AnalysisResultsConcrete(timeToHalt(e), timeToPublish(e), effects(e), publications(e), forces(e), freeVars(e))
  }
  
  // TODO: This is missing cases throughout and needs to be checked for currectness in all analyses!!
  
  def timeToHalt(e : WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ => Delay.NonBlocking
      case f || g => f.timeToHalt max g.timeToHalt
      case f > x > g if f.publications only 0 => f.timeToHalt
      case f > x > g => f.timeToHalt max g.timeToHalt
      case LimitAt(f) => f.timeToHalt min f.timeToPublish
      case Force(x: BoundVar) in ctx => ctx(x) match {
        case Bindings.SeqBound(_, _) => Delay.NonBlocking
        case _ => Delay.Blocking
      }
      case Force(x: Constant) in ctx => Delay.NonBlocking
      case Future(f) in ctx => (f in ctx).timeToHalt
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        val siteRunTime = (target match {
          case Constant(s : Site) => s.timeToHalt min args.foldRight(Delay.Blocking: Delay){ (a, others) => 
            if(a.publications only 0) a.timeToHalt min others 
            else others
          }
          case v : BoundVar if v.publications only 0 => v.timeToHalt
          case _ => Delay.Blocking
        }) 
        val argForceTime = args.foldRight(Delay.NonBlocking: Delay)((a, acc) => (a.timeToPublish min a.timeToHalt) max acc)
        siteRunTime max argForceTime
      }
      case DeclareDefsAt(defs, defsctx, body) => body.timeToHalt
      case DeclareTypeAt(_, _, b) => b.timeToHalt
      case HasType(b, _) in ctx => (b in ctx).timeToHalt
      case Constant(_) in _ => Delay.NonBlocking
      case (v : BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _) 
           | Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => Delay.NonBlocking 
        case _ => Delay.Blocking
        // FIXME: There should be cases for Bindings.DefBound(_, _, _) | Bindings.RecursiveDefBound(_, _, _)
      }
      case _ => Delay.Blocking
    }
  }
  
  def timeToPublish(e : WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ => Delay.Forever
      case f || g => f.timeToPublish min g.timeToPublish
      case f > x > g => f.timeToPublish max g.timeToPublish
      case LimitAt(f) => f.timeToPublish
      case Force(x: BoundVar) in ctx => ctx(x) match {
        case Bindings.SeqBound(_, _) => Delay.NonBlocking
        case _ => Delay.Blocking
      }
      case Force(x: Constant) in ctx => Delay.NonBlocking
      case Future(f) in ctx => Delay.NonBlocking
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx

        target match {
          case Constant(s: Site) => s.timeToPublish max args.foldRight(Delay.NonBlocking: Delay)((a, acc) => a.timeToPublish max acc)
          case v: BoundVar => Delay.Blocking
          case _ => Delay.Blocking
        }
      }
      case DeclareDefsAt(defs, _, body) => body.timeToPublish
      case DeclareTypeAt(_, _, b) => b.timeToPublish
      case HasType(b, _) in ctx => (b in ctx).timeToPublish
      case Constant(_) in _ => Delay.NonBlocking
      case (v : BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _) 
           | Bindings.DefBound(_, _, _) 
           | Bindings.RecursiveDefBound(_, _, _) => Delay.NonBlocking
        case _ => Delay.Blocking
        // FIXME: There should be cases for Bindings.DefBound(_, _, _) | Bindings.RecursiveDefBound(_, _, _)
      }
      case _ => Delay.Blocking
    }
  }
 
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
  
  def freeVars(e : WithContext[Expression]): Set[Var] =  {
    import ImplicitResults._
    e match {
      case Stop() in _ => Set()
      case f || g => f.freeVars | g.freeVars
      case f > x > g => f.freeVars | (g.freeVars - x)
      case LimitAt(f) => f.freeVars
      case Force(a) in ctx => (a in ctx).freeVars
      case Future(f) in ctx => (f in ctx).freeVars
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        target.freeVars | args.map(_.freeVars).reduce(_ | _)
      }
      case DeclareDefsAt(defs, defsctx, body) => {
        implicit val _ctx = defsctx
        val defVars = defs.map(_.name)
        (body.freeVars -- defVars) | defs.map({ d => 
          d.body.freeVars -- d.formals -- defVars
        }).reduce(_ | _)
      }
      case DeclareTypeAt(_, _, b) => b.freeVars
      case HasType(b, _) in ctx => (b in ctx).freeVars
      case Constant(_) in _ => Set()
      case (v : BoundVar) in ctx => Set(v)
    }    
  }

  def forces(e : WithContext[Expression]): Map[Var, ForceType] =  {
    import ImplicitResults._
    e match {
      case Stop() in _ => Map()
      case CallAt(target in _, args, _, ctx) => target match {
        case Constant(s : Site) => {
          val vars = (args collect { case v : Var => v })
          val mayStops = vars filter { v => !((v in ctx).publications > 0) }
          mayStops match {
            case List() => vars.map(v => (v, ForceType.Immediately(true))).toMap
            case List(v) => Map((v, ForceType.Immediately(true)))
            case _ => Map()
          }
        }
        case v : BoundVar => {
          val callstr = ctx(v) match {
            case Bindings.DefBound(ctx, _, d) => d in ctx match {
              case DefAt(name in _, formals, body, _, _, _, _) => {
                assert(name == v)
                (for((f, a: Var) <- formals zip args) yield {
                  (a, body.forces(f))
                }).toMap
              }
            }
            case _ => Map[Var, ForceType]()
          }
          callstr.updated(v, ForceType.Immediately(true))
        }
        case v : Var => Map((v, ForceType.Immediately(true)))
        case _ => Map()
      }
      case f || g => ForceType.maxMaps(f.forceTypes, g.forceTypes)
      // Adding f.publishesAtLeast(1) means that this expression cannot halt without forcing.
      case f > x > g if f.effects == Effects.None => ForceType.maxMaps(f.forceTypes, g.forceTypes - x) 
      case f > x > g => f.forceTypes.mapValues { t => t.delayBy(f.timeToPublish) }
      case LimitAt(f) => f.forceTypes
      case Force(a: Var) in ctx => Map((a, ForceType.Immediately(true)))
      case Force(a) in _ => Map()
      case Future(f) in ctx => (f in ctx).forceTypes.mapValues { t => t max ForceType.Immediately(false) }
      case DeclareDefsAt(defs, defsctx, body) => body.forceTypes
      case DeclareTypeAt(_, _, b) => b.forceTypes
      case HasType(b, _) in ctx => (b in ctx).forceTypes
      case (v : BoundVar) in _ => Map((v, ForceType.Immediately(true)))
    }
  }
  
  def effects(e : WithContext[Expression]): Effects = {
    import ImplicitResults._
    e match {
      case Stop() in _ => Effects.None
      case CallAt(target in _, args, _, _) => (target match {
        case Constant(s : Site) => s.effects
        case _ => Effects.Anytime
      })
      case f || g => f.effects max g.effects
      case f > x > g if f.publications only 0 => f.effects
      case f > x > g => f.effects match {
        case Effects.Anytime => Effects.Anytime
        case _ => g.effects max Effects.BeforePub
      }
      case LimitAt(f) => f.effects min Effects.BeforePub
      case Force(a) in _ => Effects.None
      case Future(f) in ctx => (f in ctx).effects match {
        case Effects.BeforePub => Effects.Anytime
        case e => e
      }
      case DeclareDefsAt(defs, defsctx, body) => body.effects
      case DeclareTypeAt(_, _, b) => b.effects
      case HasType(b, _) in ctx => (b in ctx).effects
      case Constant(_) in _ => Effects.None
      case (v : BoundVar) in _ => Effects.None
      case _ => Effects.Anytime
    }
  }
}
