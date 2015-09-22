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

sealed trait ForceType {
  val haltsWith = false
  def max(o: ForceType): ForceType
  def min(o: ForceType): ForceType

  def nonHalting: ForceType = this
  def delayBy(d: Delay): ForceType = d match {
    case Delay.NonBlocking => this
    case Delay.Blocking => this max ForceType.Eventually
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

  /** Does this expression force the variable <code>x</code> before performing any visible
    * action other than halting?
    * Specifically is <code>x</code> forced before the expression has side effects or publishes.
    */
  def forces(x: Var) = forceTypes.getOrElse(x, ForceType.Never)
  def forceTypes: Map[Var, ForceType]

  /** Free variables in the expression.
    */
  def freeVars: Set[Var]
  
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
  forceTypes: Map[Var, ForceType],
  freeVars: Set[Var],
  valueForceDelay: Delay) extends AnalysisResults {
  // TODO: Add validity checks to catch unreasonable combinations of values
}

sealed trait ExpressionAnalysisProvider[E <: Expression] {
  outer =>
  def apply(e: E)(implicit ctx: TransformContext): AnalysisResults
  def apply(e: WithContext[E]): AnalysisResults = this(e.e)(e.ctx)
  def get(e: E)(implicit ctx: TransformContext): Option[AnalysisResults]

  object ImplicitResults {
    import scala.language.implicitConversions
    @inline
    implicit def expressionWithResults(e: E)(implicit ctx: TransformContext): AnalysisResults = apply(e)
    @inline
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
    AnalysisResultsConcrete(timeToHalt(e), timeToPublish(e), effects(e), publications(e), forces(e), freeVars(e),
        valueForceDelay(e))
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
      case f ConcatAt g =>
        f.timeToHalt max g.timeToHalt
      case LimitAt(f) =>
        f.timeToHalt min f.timeToPublish
      case Force(x) in ctx =>
        (x in ctx).valueForceDelay
      case Future(f) in ctx =>
        (f in ctx).timeToHalt
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        val siteRunTime = (target match {
          case Constant(s: Site) => s.timeToHalt min args.foldRight(Delay.Blocking: Delay) { (a, others) =>
            if (a.publications only 0) a.valueForceDelay min others
            else others
          }
          // TODO: Needs cases for def calls.
          case _ => 
            Delay.Blocking
        })
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
      case f ConcatAt g =>
        f.timeToPublish min (f.timeToHalt max g.timeToPublish)
      case LimitAt(f) =>
        f.timeToPublish
      case Force(x: BoundVar) in ctx =>
        ctx(x) match {
          case Bindings.SeqBound(sctx, Sequence(Future(source), _, _)) =>
            (source in sctx).timeToPublish
          case Bindings.DefBound(_, _, _)
             | Bindings.RecursiveDefBound(_, _, _) =>
            (x in ctx).valueForceDelay
          case _ =>
            Delay.Blocking
        }
      case Force(x: Constant) in ctx =>
        Delay.NonBlocking
      case Future(f) in ctx =>
        Delay.NonBlocking
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx

        target match {
          case Constant(s: Site) => 
            s.timeToPublish max args.foldRight(Delay.NonBlocking: Delay) { 
              (a, acc) => a.valueForceDelay max acc 
            }
          case v: BoundVar => 
            Delay.Blocking
          case _ => Delay.Blocking
        }
      }
      case DeclareDefsAt(defs, _, body) =>
        body.timeToPublish
      case DeclareTypeAt(_, _, b) =>
        b.timeToPublish
      case HasType(b, _) in ctx =>
        (b in ctx).timeToPublish
      case Constant(_) in _ =>
        Delay.NonBlocking
      case (v: BoundVar) in ctx => ctx(v) match {
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
    }
  }

  def publications(e: WithContext[Expression]): Range = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Range(0, 0)
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        (target match {
          case Constant(s: Site) => {
            if (args.forall(a => a.publications > 0))
              s.publications
            else if (args.exists(a => a.publications only 0))
              Range(0, 0)
            else
              s.publications.mayHalt
          }
          case _ => Range(0, None)
        })
      }
      case LimitAt(f) =>
        f.publications.limitTo(1)
      case Force(x: BoundVar) in ctx => ctx(x) match {
        case Bindings.DefBound(_, _, _)
          | Bindings.RecursiveDefBound(_, _, _) => 
          Range(1, 1)
        case Bindings.SeqBound(sctx, Sequence(Future(source), _, _)) =>
          (source in sctx).publications.limitTo(1)
        case _ => 
          Range(0, 1)
      }
      case Force(Constant(_)) in ctx => 
        Range(1, 1)
      case Force(_) in ctx => 
        Range(0, 1)
      case Future(f) in _ =>
        Range(1, 1)
      case f || g =>
        f.publications + g.publications
      case f ConcatAt g =>
        f.publications + g.publications
      case f > x > g =>
        f.publications * g.publications
      case DeclareDefsAt(defs, defsctx, body) => {
        body.publications
      }
      case DeclareTypeAt(_, _, b) =>
        b.publications
      case HasType(b, _) in ctx =>
        (b in ctx).publications
      case Constant(_) in _ => Range(1, 1)
      case (v: BoundVar) in ctx => ctx(v) match {
        case Bindings.SeqBound(_, _)
          | Bindings.DefBound(_, _, _)
          | Bindings.RecursiveDefBound(_, _, _) =>
          Range(1, 1)
        case Bindings.ArgumentBound(_, _, _) => 
          Range(1, 1)
      }
      case FieldAccess(_, _) in _ =>
        Range(0, 1)
    }
  }

  def freeVars(e: WithContext[Expression]): Set[Var] = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Set()
      case f || g =>
        f.freeVars | g.freeVars
      case f ConcatAt g =>
        f.freeVars | g.freeVars
      case f > x > g =>
        f.freeVars | (g.freeVars - x)
      case LimitAt(f) =>
        f.freeVars
      case Force(a) in ctx =>
        (a in ctx).freeVars
      case Future(f) in ctx =>
        (f in ctx).freeVars
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        target.freeVars | args.map(_.freeVars).foldRight(Set[Var]())(_ | _)
      }
      case DeclareDefsAt(defs, defsctx, body) => {
        implicit val _ctx = defsctx
        val defVars = defs.map(_.name) 
        
        val defFreeVars = (for(d <- defs.iterator) yield {
          val DefAt(_, formals, body, _, _, _, _) = d in defsctx
          body.freeVars -- formals
        }).reduce(_ | _)
            
        (body.freeVars | defFreeVars) -- defVars
      }
      case DeclareTypeAt(_, _, b) =>
        b.freeVars
      case HasType(b, _) in ctx =>
        (b in ctx).freeVars
      case Constant(_) in _ =>
        Set()
      case (v: BoundVar) in ctx =>
        Set(v)
      case FieldAccess(v, _) in ctx =>
        (v in ctx).freeVars
    }
  }

  def forces(e: WithContext[Expression]): Map[Var, ForceType] = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Map()
      case Constant(_) in _ =>
        Map()
      case CallAt(target in _, args, _, ctx) => target match {
        case Constant(s: Site) => {
          val vars = (args collect { case v: Var => v })
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
                (for ((f, a: Var) <- formals zip args) yield {
                  (a, body.forces(f))
                }).toMap
              }
            }
            case _ => Map[Var, ForceType]()
          }
          callstr.updated(v, ForceType.Immediately(true))
        }
        case v: Var => Map((v, ForceType.Immediately(true)))
        case _ => Map()
      }
      case f || g =>
        ForceType.mergeMaps(
            _ max _, ForceType.Immediately(false), 
            f.forceTypes, g.forceTypes)
      // Adding f.publishesAtLeast(1) means that this expression cannot halt without forcing.
      case f ConcatAt g =>
        ForceType.mergeMaps(
            _ min _, ForceType.Never,
            f.forceTypes, g.forceTypes.mapValues { t => t.delayBy(f.timeToHalt) })
      case f > x > g =>
        ForceType.mergeMaps(
            _ min _, ForceType.Never,
            f.forceTypes, (g.forceTypes - x).mapValues { t => t.delayBy(f.timeToPublish) })
      case LimitAt(f) =>
        f.forceTypes
      case Force(a: Var) in ctx =>
        Map((a, ForceType.Immediately(true)))
      case Force(a) in _ =>
        Map()
      case Future(f) in ctx =>
        (f in ctx).forceTypes.mapValues { t => t max ForceType.Immediately(false) }
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
    }
  }

  def effects(e: WithContext[Expression]): Effects = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Effects.None
      case CallAt(target in _, args, _, _) => (target match {
        case Constant(s: Site) => s.effects
        case _ => Effects.Anytime
      })
      case f || g =>
        f.effects max g.effects
      case f > x > g if f.publications only 0 =>
        f.effects
      case f > x > g => 
        g.effects max f.effects
      case f ConcatAt g => f.effects match {
        case Effects.Anytime if f.publications only 0 =>
          g.effects max Effects.BeforePub
        case _ =>
          g.effects max f.effects
      }
      case LimitAt(f) =>
        f.effects min Effects.BeforePub
      case Force(a) in _ =>
        Effects.None
      case Future(f) in ctx => (f in ctx).effects match {
        case Effects.BeforePub =>
          Effects.Anytime
        case e =>
          e
      }
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
    }
  }

  def valueForceDelay(e: WithContext[Expression]): Delay = {
    import ImplicitResults._
    e match {
      case Stop() in _ =>
        Delay.NonBlocking
      case CallAt(target in _, args, _, ctx) => 
        target match {
          case Constant(s: Site) => 
            Delay.NonBlocking // Sites never make futures
          case v: BoundVar => ctx(v) match {
            case Bindings.DefBound(ctx, decls, d) => {
              val DeclareDefsAt(_, dctx, _) = decls in ctx
              val DefAt(_, _, body, _, _, _, _) = d in dctx
              body.valueForceDelay
            }
            case _ => Delay.Blocking
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
      case f ConcatAt g => 
        // TODO: Can Blocking actually be treated as the join of other elements in Delay?
        if (f.valueForceDelay == g.valueForceDelay)
          g.valueForceDelay
        else
          Delay.Blocking
      case LimitAt(f) =>
        f.valueForceDelay
      case Force(a) in _ =>
        Delay.NonBlocking
      case Future(f) in ctx =>
        (f in ctx).timeToPublish min (f in ctx).timeToHalt
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
          val closedVars = freeVars(body) -- d.formals -- decls.defs.map(_.name)
          val closedVarDelay = (for (x <- closedVars.iterator) yield {
            (x in body.ctx).valueForceDelay
          }).foldLeft(Delay.NonBlocking: Delay)(_ max _)
          closedVarDelay
        }
        
        ctx(x) match {
          case Bindings.SeqBound(sctx, s) =>
            (s.left in sctx).valueForceDelay
          case Bindings.DefBound(ctx, decls, d) => 
            handleDef(decls, ctx, d)
          case Bindings.RecursiveDefBound(ctx, decls, d) =>
            // TODO: Make free variables somehow computed first so this can be used: handleDef(decls, ctx, d)
            Delay.Blocking
          case _ =>
            Delay.Blocking
        }
      case FieldAccess(_, _) in _ =>
        Delay.NonBlocking
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