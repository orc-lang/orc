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
import orc.values.sites.Range
import orc.values.sites.SiteMetadata

sealed trait ForceType {
  val haltsWith = false
  def max(o: ForceType): ForceType
  def min(o: ForceType): ForceType

  def withHalting(b: Boolean): ForceType = this
  def delayBy(d: Delay): ForceType = d match {
    case Delay.NonBlocking => this
    case Delay.Blocking => this max ForceType.Eventually(this.haltsWith)
    case Delay.Forever => this max ForceType.Never
  }
 
  def <=(o: ForceType): Boolean = {
    (this max o) == o
  }
}
object ForceType {
  case class Immediately(override val haltsWith: Boolean) extends ForceType {
    def max(o: ForceType): ForceType = o match {
      case Immediately(_) => Immediately(haltsWith && o.haltsWith)
      case _ => o
    }
    def min(o: ForceType): ForceType = o match {
      case Immediately(_) => Immediately(haltsWith || o.haltsWith)
      case _ => this
    }

    override def withHalting(b: Boolean): ForceType = Immediately(b)
  }
  case class Eventually(override val haltsWith: Boolean) extends ForceType {
    def max(o: ForceType): ForceType = o match {
      case Never => o
      case _ => Eventually(haltsWith && o.haltsWith)
    }
    def min(o: ForceType): ForceType = o match {
      case Immediately(_) => Immediately(haltsWith || o.haltsWith)
      case _ => this
    }
    
    override def withHalting(b: Boolean): ForceType = Eventually(b)
  }
  case object Never extends ForceType {
    def max(o: ForceType): ForceType = this
    def min(o: ForceType): ForceType = o
  }

  def mergeMaps[T](op: (ForceType, ForceType) => ForceType, default: ForceType, a: Map[T, ForceType], b: Map[T, ForceType]): Map[T, ForceType] = {
    val res = for (e <- a.keysIterator ++ b.keysIterator) yield (e, op(a.getOrElse(e, default), b.getOrElse(e, default)))
    res.toMap
  }
  /*
  def maxMaps[T](a: Map[T, ForceType], b: Map[T, ForceType]): Map[T, ForceType] = 
    intersectMap(_ max _, a, b)
  def minMaps[T](a: Map[T, ForceType], b: Map[T, ForceType]): Map[T, ForceType] = 
    intersectMap(_ min _, a, b)
  */
}

/*
sealed trait ValueKind {
  def join(o: ValueKind) = {
    if(o == this)
      this
    else
      ValueKind.Unknown
  }
}
object ValueKind {
  case object Unknown extends ValueKind
  case object Blocking extends ValueKind
  case object NonBlocking extends ValueKind 
  
  def apply(d: Delay) = d match {
    case Delay.Blocking => Blocking
    case Delay.NonBlocking => NonBlocking
    case Delay.Forever => Unknown
  }
}
*/

/** The analysis results of an expression. The results refer to this object used an
  * expression. So for variables publication is in terms of what happens when you
  * access the variable and publish the value you get (or don't get).
  */
trait AnalysisResults {
  /** Does this expression halt immediately?
    */
  def timeToHalt: Delay

  /** Does this expression publish immediately?
    */
  def timeToPublish: Delay

  /** This expression only has side-effects before this time.
    */
  def effects: Effects

  /** How many times will this expression publish?
    */
  def publications: Range
  
  /** Metadata on the site published by this expression if available.
    */
  def siteMetadata: Option[SiteMetadata]

  /** Does this expression force the variable <code>x</code> before performing any visible
    * action other than halting?
    * Specifically is <code>x</code> forced before the expression has side effects or publishes.
    */
  def forces(x: BoundVar) = forceTypes.getOrElse(x, ForceType.Never)
  def forceTypes: Map[BoundVar, ForceType]
  
  // TODO: This may not be the right way to represent this.
  /*   For each future I basically want to know full analyses of what a force on it will do.
   *   Maybe I should just create phantom force operations and have the force case in each analyses
   *   look at the source of forced value.
   */
  /** The amount of time forcing values published by this expression will cost.
    */
  def valueForceDelay: Delay
  
  def silent = publications only 0
  def talkative = publications > 0
  
  def effectFree = effects == Effects.None
  
  def nonBlockingHalt = timeToHalt == Delay.NonBlocking
  def nonBlockingPublish = timeToPublish == Delay.NonBlocking
}

case class AnalysisResultsConcrete(
  timeToHalt: Delay,
  timeToPublish: Delay,
  effects: Effects,
  publications: Range,
  forceTypes: Map[BoundVar, ForceType],
  valueForceDelay: Delay,
  siteMetadata: Option[SiteMetadata]) extends AnalysisResults {
  // TODO: Add validity checks to catch unreasonable combinations of values
}

sealed trait ExpressionAnalysisProvider[E <: Expression] {
  outer =>
  def apply(e: E)(implicit ctx: TransformContext): AnalysisResults
  def apply(e: WithContext[E]): AnalysisResults = this(e.e)(e.ctx)
  def get(e: E)(implicit ctx: TransformContext): Option[AnalysisResults]

  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionWithResults(e: E)(implicit ctx: TransformContext): AnalysisResults = apply(e)
    implicit def expressionCtxWithResults(e: WithContext[E]): AnalysisResults = apply(e.e)(e.ctx)
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

/** A cache for storing all the results of a bunch of expressions.
  */
class ExpressionAnalyzer extends ExpressionAnalysisProvider[Expression] {
  val cache = mutable.Map[(TransformContext, Expression), AnalysisResults]()
  def apply(e: Expression)(implicit ctx: TransformContext) = {
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
  def get(e: Expression)(implicit ctx: TransformContext) = Some(this(e))

  // TODO: Recursive functions could be handled by thinking of these functions as recursive relations on the analysis result.
  // These recursive relations could either be solve in place as we go or reified and then solved using any technique.

  // TODO: The analysis I am doing is flow-sensative and outward-context-sensative in that functions are 
  //       analyzed for each call site and that result used at that site. However no information flow 
  //       inward from the call site to allow optimization inside the function body. A conscious decision 
  //       should be made as to what sensativities we want.

  // TODO: All these analyses are really flow analyses of one form or another. This should use a consistant 
  //       framework for them. However because we have no goto all flow control is through functions which
  //       means flow analyses MUST occur across function boundries.

  def analyze(e: WithContext[Expression]): AnalysisResults = {
    AnalysisResultsConcrete(timeToHalt(e), timeToPublish(e), effects(e), publications(e), forces(e), valueForceDelay(e), siteMetadata(e))
  }

  // TODO: This is missing cases throughout and needs to be checked for currectness in all analyses!!

  
  // TODO: For correctness I need to assume values to calls are futures. So I need to block on them at future call time.
  
  def timeToHalt(e: WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Delay.NonBlocking
      case f || g =>
        f.timeToHalt max g.timeToHalt
      case f > x > g if f.publications only 0 =>
        f.timeToHalt
      case f > x > g =>
        f.timeToHalt max g.timeToHalt
      case FutureAt(x, f, g) =>
        f.timeToHalt max g.timeToHalt
      case f OtherwiseAt g =>
        f.timeToHalt max g.timeToHalt // TODO: IT may be possible to tighten this.
      case LimitAt(f) =>
        f.timeToHalt min f.timeToPublish
      case Force(x, _) in ctx =>
        (x in ctx).valueForceDelay
      case CallAt(target, args, _, ctx) => {
        implicit val _ctx = ctx
        // TODO: Needs cases for def calls.
        val siteRunTime = target.siteMetadata.map(s => {
          s.timeToHalt min args.foldRight(Delay.Blocking: Delay) { (a, others) =>
            if (a.publications only 0) a.valueForceDelay min others
            else others
          }
        }).getOrElse(Delay.Blocking)
        val argForceTime = args.foldRight(Delay.NonBlocking: Delay) {
          (a, acc) => a.valueForceDelay max acc
        }
        siteRunTime max argForceTime
      }
      case DeclareDefsAt(defs, defsctx, body) =>
        body.timeToHalt
      case DeclareTypeAt(_, _, b) =>
        b.timeToHalt
      case HasType(b, _) in ctx =>
        (b in ctx).timeToHalt
      case Constant(_) in _ =>
        Delay.NonBlocking
      case (v: BoundVar) in ctx =>
        ctx(v) match {
          case Bindings.SeqBound(_, _)
            | Bindings.DefBound(_, _, _)
            | Bindings.RecursiveDefBound(_, _, _) => 
            Delay.NonBlocking
          case Bindings.ArgumentBound(_, _, _) => 
            Delay.NonBlocking
          case _ => 
            Delay.Blocking
        }
      case FieldAccess(_, _) in _ =>
        Delay.NonBlocking
      case VtimeZone(_, e) in ctx =>
        (e in ctx).timeToHalt
    }
  }

  def timeToPublish(e: WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Delay.Forever
      case f || g =>
        f.timeToPublish min g.timeToPublish
      case f > x > g =>
        f.timeToPublish max g.timeToPublish
      case FutureAt(x, f, g) =>
        g.timeToPublish
      case f OtherwiseAt g =>
        f.timeToPublish min (f.timeToHalt max g.timeToPublish)
      case LimitAt(f) =>
        f.timeToPublish min f.timeToHalt
      case Force(x: BoundVar, _) in ctx =>
        ctx(x) match {
          case Bindings.FutureBound(sctx, Future(_, source, _)) =>
            (source in sctx).timeToPublish
          case Bindings.DefBound(_, _, _)
             | Bindings.RecursiveDefBound(_, _, _) =>
            (x in ctx).valueForceDelay
          case _ =>
            Delay.Blocking
        }
      case Force(x: Constant, _) in ctx =>
        Delay.NonBlocking
      case CallAt(target, args, _, ctx) => {
        implicit val _ctx = ctx
        target.siteMetadata.map(s => {
          s.timeToPublish max args.foldRight(Delay.NonBlocking: Delay) { 
            (a, acc) => a.valueForceDelay max acc 
          }
        }).getOrElse(Delay.Blocking)
      }
      case DeclareDefsAt(defs, _, body) =>
        body.timeToPublish
      case DeclareTypeAt(_, _, b) =>
        b.timeToPublish
      case HasType(b, _) in ctx =>
        (b in ctx).timeToPublish
      case Constant(_) in _ =>
        Delay.NonBlocking
      case (v: BoundVar) in ctx => 
        Delay.NonBlocking
      case FieldAccess(_, _) in _ =>
        Delay.NonBlocking
      case VtimeZone(_, e) in ctx =>
        (e in ctx).timeToPublish
    }
  }

  def publications(e: WithContext[Expression]): Range = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Range(0, 0)
      case CallAt(target, args, _, ctx) => {
        implicit val _ctx = ctx
        target.siteMetadata.map(s => {
            if (args.forall(a => a.publications > 0))
              s.publications
            else if (args.exists(a => a.publications only 0))
              Range(0, 0)
            else
              s.publications.mayHalt
        }).getOrElse(Range(0, None))
      }
      case LimitAt(f) =>
        f.publications.limitTo(1)
      case Force(x: BoundVar, _) in ctx => ctx(x) match {
        case Bindings.DefBound(_, _, _)
          | Bindings.RecursiveDefBound(_, _, _) => 
          Range(1, 1)
        case Bindings.FutureBound(sctx, Future(_, source, _)) =>
          (source in sctx).publications.limitTo(1)
        case _ => 
          Range(0, 1)
      }
      case Force(Constant(_), _) in ctx => 
        Range(1, 1)
      case Force(_, _) in ctx => 
        Range(0, 1)
      case f || g =>
        f.publications + g.publications
      case f OtherwiseAt g =>
        f.publications.intersectOption(Range(1, None)).getOrElse(g.publications) union g.publications
      case f > x > g =>
        f.publications * g.publications
      case FutureAt(x, f, g) =>
        g.publications
      case DeclareDefsAt(defs, defsctx, body) => {
        body.publications
      }
      case DeclareTypeAt(_, _, b) =>
        b.publications
      case HasType(b, _) in ctx =>
        (b in ctx).publications
      case Constant(_) in _ => Range(1, 1)
      case (v: BoundVar) in ctx => Range(1, 1)
      case FieldAccess(target, f) in ctx =>
        val fieldMetadata = (target in ctx).siteMetadata.flatMap(_.fieldMetadata(f))
        if (fieldMetadata.isDefined)
          Range(1, 1)
        else
          Range(0, 1)
      case VtimeZone(_, e) in ctx =>
        (e in ctx).publications
    }
  }

  def forces(e: WithContext[Expression]): Map[BoundVar, ForceType] = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Map()
      case Constant(_) in _ =>
        Map()
      case CallAt(target in _, args, _, ctx) => target match {
        case t if (t in ctx).siteMetadata.isDefined => {
          val vars = (args collect { case v: BoundVar => v })
          val mayStops = vars filter { v => !((v in ctx).publications > 0) }
          mayStops match {
            case List() => vars.map(v => (v, ForceType.Immediately(true))).toMap
            case List(v) => Map((v, ForceType.Immediately(true)))
            case _ => Map()
          }
        }
        case v: BoundVar => {
          val callstr = ctx(v) match {
            case Bindings.DefBound(ctx, _, d) => d in ctx match {
              case DefAt(name in _, formals, body, _, _, _, _) => {
                assert(name == v)
                (for ((f, a: BoundVar) <- formals zip args) yield {
                  (a, body.forces(f))
                }).toMap
              }
            }
            // TODO: The system really needs some limited recursive analysis.
//            case Bindings.RecursiveDefBound(ctx, _, d) => d in ctx match {
//              case DefAt(name in _, formals, body, _, _, _, _) => {
//                assert(name == v)
//              }
//            }
            case _ => Map[BoundVar, ForceType]()
          }
          callstr.updated(v, ForceType.Immediately(true))
        }
        case _ => Map()
      }
      case f || g =>
        ForceType.mergeMaps(
            _ max _, ForceType.Never, 
            f.forceTypes, g.forceTypes)
      // Adding f.publishesAtLeast(1) means that this expression cannot halt without forcing.
      case f OtherwiseAt g =>
        // Take the minimum force of f and g (shifted by the halt time of f).
        // And never claim to halt with anything in g.
        if (f.publications only 0)
          ForceType.mergeMaps(
              _ min _, ForceType.Never,
              f.forceTypes, g.forceTypes.mapValues { t => t.delayBy(f.timeToHalt) }).mapValues { _.withHalting(false) }
        else
          ForceType.mergeMaps(
              _ min _, ForceType.Never,
              f.forceTypes, g.forceTypes.mapValues { t => t max ForceType.Eventually(false) }).mapValues { _.withHalting(false) }
      case f > x > g =>
        ForceType.mergeMaps(
            _ min _, ForceType.Never,
            f.forceTypes, (g.forceTypes - x).mapValues { t => t.delayBy(f.timeToPublish) })
      case LimitAt(f) =>
        f.forceTypes
      case Force(a: BoundVar, _) in ctx =>
        Map((a, ForceType.Immediately(true)))
        // TODO: These may also wait on the content of closures which could be useful. (for functions returning functions for instance)
      case Force(a, _) in _ =>
        Map()
      case FutureAt(x, f, g) =>
        val fForces = f.forceTypes.mapValues { t => t max ForceType.Eventually(false) }
        ForceType.mergeMaps(
            _ max _, ForceType.Never, 
            fForces, g.forceTypes)
      case DeclareDefsAt(defs, defsctx, body) =>
        body.forceTypes
      case DeclareTypeAt(_, _, b) =>
        b.forceTypes
      case HasType(b, _) in ctx =>
        (b in ctx).forceTypes
      case (v: BoundVar) in _ =>
        Map()
      case FieldAccess(_, _) in _ =>
        Map()
      case VtimeZone(_, _) in _ =>
        Map()
    }
  }
  
  def effects(e: WithContext[Expression]): Effects = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Effects.None
      case CallAt(target, args, _, _) => 
        target.siteMetadata.map(_.effects).getOrElse(Effects.Anytime)
      case f || g =>
        f.effects max g.effects
      case f > x > g if f.publications only 0 =>
        f.effects
      case f > x > g => 
        g.effects max f.effects
      case f OtherwiseAt g => f.effects match {
        case Effects.Anytime if f.publications only 0 =>
          g.effects max Effects.BeforePub
        case _ =>
          g.effects max f.effects
      }
      case LimitAt(f) =>
        f.effects min Effects.BeforePub
      case Force(a, _) in _ =>
        Effects.None
      case FutureAt(x, f, g) => g.effects max (f.effects match {
        case Effects.BeforePub =>
          Effects.Anytime
        case e =>
          e
      })
      case DeclareDefsAt(defs, defsctx, body) =>
        body.effects
      case DeclareTypeAt(_, _, b) =>
        b.effects
      case HasType(b, _) in ctx =>
        (b in ctx).effects
      case Constant(_) in _ =>
        Effects.None
      case (v: BoundVar) in _ =>
        Effects.None
      case FieldAccess(_, _) in _ =>
        Effects.None
      case VtimeZone(_, e) in ctx =>
        (e in ctx).effects
    }
  }

  def valueForceDelay(e: WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Delay.NonBlocking
      case CallAt(target in _, args, _, ctx) => 
        target match {
          case t if (t in ctx).siteMetadata.isDefined =>
            Delay.NonBlocking // Sites never make futures
          case v: BoundVar => ctx(v) match {
            // TODO: I need to prove that all calls will actually return concrete values that will not block.
            case Bindings.DefBound(_, _, _) | 
                 Bindings.RecursiveDefBound(_, _, _) => {
              Delay.NonBlocking
            }
            // TODO: Prove that no called value will publish a future.
            case _ => Delay.NonBlocking
          }
          case _ => Delay.Blocking
        }        
        // TODO: Figure out if this should be required to be NonBlocking
        // This restricts all calls (sites and defs) from publishing futures
      case f || g =>
        // TODO: Can Blocking actually be treated as the join of other elements in Delay?
        if (f.valueForceDelay == g.valueForceDelay)
          g.valueForceDelay
        else
          Delay.Blocking
      case f > x > g => 
        g.valueForceDelay
      case f OtherwiseAt g => 
        // TODO: Can Blocking actually be treated as the join of other elements in Delay?
        if (f.valueForceDelay == g.valueForceDelay)
          g.valueForceDelay
        else
          Delay.Blocking
      case LimitAt(f) =>
        f.valueForceDelay
      case Force(a, _) in _ =>
        Delay.NonBlocking
      case FutureAt(x, f, g) =>
        g.valueForceDelay
      case DeclareDefsAt(defs, defsctx, body) =>
        body.valueForceDelay
      case DeclareTypeAt(_, _, b) =>
        b.valueForceDelay
      case HasType(b, _) in ctx =>
        (b in ctx).valueForceDelay
      case Constant(_) in _ =>
        Delay.NonBlocking
      case (x: BoundVar) in ctx => 
        def handleDef(decls: DeclareDefs, ctx: TransformContext, d: Def) = {
          val DeclareDefsAt(_, dctx, _) = decls in ctx
          val DefAt(_, _, body, _, _, _, _) = d in dctx
          // Remove all arguments because they are not closed variables.
          // Remove all recursive references. Because those will always 
          // be available immediately when non-recursive references 
          // become available, so they can never increase the delay.
          val closedVars = body.freeVars -- d.formals -- decls.defs.map(_.name)
          val closedVarDelay = (for (x <- closedVars.iterator) yield {
            (x in body.ctx).valueForceDelay
          }).foldLeft(Delay.NonBlocking: Delay)(_ max _)
          closedVarDelay
        }
        
        ctx(x) match {
          case Bindings.SeqBound(sctx, s) =>
            Delay.NonBlocking
          case Bindings.FutureBound(sctx, Future(_, s, _)) =>
            // This could sort of be: (f in ctx).timeToPublish min (f in ctx).timeToHalt
            // However in the optimizer uses valueForceDelay to detect if force can be removed.
            // So futures should never be NonBlocking
            ((s in sctx).timeToPublish min (s in sctx).timeToHalt) max Delay.Blocking          
          case Bindings.DefBound(ctx, decls, d) => 
            handleDef(decls, ctx, d)
          case Bindings.RecursiveDefBound(ctx, decls, d) =>
            handleDef(decls, ctx, d)
          case _ =>
            Delay.Blocking
        }
      case FieldAccess(_, _) in _ =>
        Delay.NonBlocking
      case VtimeZone(_, _) in _ =>
        Delay.Blocking
    }
  }
  
  def siteMetadata(e: WithContext[Expression]): Option[SiteMetadata] = {
    import ImplicitResults._
    e match {
      case Constant(s: SiteMetadata) in _ => Some(s)
      case (x: BoundVar) in ctx => ctx(x) match {
        case Bindings.SeqBound(sctx, s) =>
          (s.left in sctx).siteMetadata
        case _ =>
          None
      }
      case CallAt(target, args, _, ctx) =>
        target.siteMetadata flatMap { sm =>
          sm.returnMetadata(args.map {
            _ match {
              case Constant(v) => Some(v)
              case _ => None
            }
          })
        }
      case FieldAccess(target, f) in ctx =>
        (target in ctx).siteMetadata flatMap { sm =>
          sm.fieldMetadata(f)
        }
      case LimitAt(f) =>
        f.siteMetadata
      case FutureAt(x, f, g) =>
        g.siteMetadata
      case f > x > g =>
        g.siteMetadata
      case _ => None
    }
  }

}

object Analysis {
  def count(t : NamedAST, p : (Expression => Boolean)) : Int = {
    val cs = t.subtrees
    (t match {
      case e : Expression if p(e) => 1
      case _ => 0
    }) +
    (cs.map( count(_, p) ).sum)
  }
  
  val futureCost = 4
  val forceCost = 4
  val sequenceCost = 1
  val limitCost = 3
  val callCost = 1
  
  def cost(t : NamedAST) : Int = {
    val cs = t.subtrees
    (t match {
      case _ : Future => futureCost
      case _ : Force => forceCost
      case _ : Sequence => sequenceCost
      case _ : Limit => limitCost
      case _ : Call => callCost
      case _ => 0
    }) +
    (cs.map( cost(_) ).sum)
  }
}