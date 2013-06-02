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
package orc.ast.oil.named.orc5c

import orc.values.sites.Site
import scala.collection.mutable
import orc.values.Field

case class Range(mini : Int, maxi : Option[Int]) {
  assert(mini >= 0)
  assert(maxi map {_ >= mini} getOrElse true)
  
  def >=(n : Int) = mini >= n
  def >(n : Int) = mini > n
  def <=(n : Int) = maxi map {_ <= n} getOrElse false
  def <(n : Int) = maxi map {_ < n} getOrElse false
  
  def only(n : Int) = mini == n && maxi == Some(n)
  
  def intersect(r : Range) : Option[Range] = {
    val n = mini max r.mini
    val m = (maxi, r.maxi) match {
      case (Some(x), Some(y)) => Some(x min y)
      case (Some(_), None) => maxi
      case (None, Some(_)) => r.maxi
      case (None, None) => None
    }
    if( m map {_ >= n} getOrElse true )
      Some(Range(n, m))
    else
      None
  }
  
  def +(r : Range) = {
    Range(mini + r.mini, (maxi, r.maxi) match {
      case (Some(n), Some(m)) => Some(n + m)
      case _ => None
    })
  }
  def *(r : Range) = {
    Range(mini * r.mini, (maxi, r.maxi) match {
      case (Some(n), Some(m)) => Some(n * m)
      case _ => None
    })
  }
  
  /**
   * Return a range that allows a maximum of n publications.
   */
  def limitTo(lim : Int) = {
    val n = mini min lim
    val m = maxi map (_ min lim) getOrElse lim
    Range(n, m)   
  }
  
  def mayHalt = {
    Range(0, maxi)
  }
}

object Range {
  def apply(n : Int, m : Int) : Range = Range(n, Some(m))
  
  def apply(r : (Int, Option[Int])) : Range = {
    Range(r._1, r._2)
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
  def immediateHalt: Boolean 
  
  /**
   * Does this expression publish immediately?
   */
  def immediatePublish: Boolean 
 
  /**
   * How many times will this expression publish?
   * The pair is minimum and maximum where None represents
   * no maximum.
   */
  def publications: Range
  
  def silent = publications only 0
  def talkative = publications >= 1
  
  def publishesAtMost(n : Int) = publications <= n
  def publishesAtLeast(n : Int) = publications >= n

  /**
   * Is this expression strict on the variable <code>x</code>?
   * In otherwords does it dereference it before doing anything else?
   */
  def strictOn(x : Var) = strictOnSet(x)
  
  def strictOnSet : Set[Var]
  
  /**
   * Does this expression have side-effects?
   */
  def effectFree: Boolean
  
  def same(r : AnalysisResults) = {
    immediateHalt == r.immediateHalt &&
    immediatePublish == r.immediatePublish &&
    publications == r.publications &&
    strictOnSet == r.strictOnSet &&
    effectFree == r.effectFree
  }
}

case class AnalysisResultsConcrete(
  immediateHalt: Boolean, 
  immediatePublish: Boolean,
  publications: Range, 
  strictOnSet: Set[Var],
  effectFree: Boolean
    ) extends AnalysisResults {
  if( immediatePublish && !publishesAtLeast(1) )
    assert(publishesAtLeast(1), "immediatePublish must imply at least one publication") 
  if( immediateHalt && publishesAtLeast(1) && !immediatePublish)
    assert(immediatePublish, "immediateHalt and at least 1 publication must imply immediatePublish") 
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
  
  def withDefault : ExpressionAnalysisProvider[E] = {
    new ExpressionAnalysisProvider[E] {
      def apply(e: E)(implicit ctx: TransformContext) : AnalysisResults = get(e).getOrElse(AnalysisResultsConcrete(false, false, Range(0, None), Set(), false))
      def get(e: E)(implicit ctx: TransformContext) : Option[AnalysisResults] = outer.get(e)
    }
  }
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
  
  def analyze(e : WithContext[Expression]) : AnalysisResults = {
    AnalysisResultsConcrete(immediateHalt(e), immediatePublish(e), publications(e), strictOn(e), effectFree(e))
  }
  
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
      case CallAt(target, List(Constant(f : Field)), _, _) => { // TODO: Verify the correctness of this.
        target.immediateHalt
      }
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
        if(defs.exists(_.name.optionalVariableName == Some("toattr")) && !v) {
          println("Here: " + (body))
        }
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
      case CallAt(target, List(Constant(f : Field)), _, _) => { // TODO: Verify the correctness of this.
        target.immediatePublish
      }
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
      }
      case _ => false
    }
  }
 
  def publications(e : WithContext[Expression]): Range = {
    import ImplicitResults._
    e match {
      case Stop() in _ => Range(0,0)
      case CallAt(target, List(Constant(f : Field)), _, _) => { // TODO: Verify the correctness of this.
        target.publications.limitTo(1)
      }
      case CallAt(target in _, args, _, ctx) => {
        implicit val _ctx = ctx
        (target match {
        case Constant(s : Site) => {
          if( args.forall(a => a.talkative) )
            Range(s.publications)
          else if( args.exists(a => a.silent) )
              Range(0, 0)
          else
            Range(s.publications).mayHalt
        }
        case _ => Range(0, None)
      })
      }
      case LimitAt(f) => f.publications.limitTo(1)
      case f || g => f.publications + g.publications
      case f ow g if f.publications.mini == 0 =>
        Range(1 min g.publications.mini, (f.publications.maxi, g.publications.maxi) match {
          case (Some(n), Some(m)) => Some(n max m)
          case _ => None
        })
      case f ow g if f.publishesAtLeast(1) => f.publications
      case f > x > g => f.publications * g.publications
      case f < x <| g => f.publications
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
        case b : Bindings.LateBound => {
          b.valueExpr.publications.limitTo(1)
        }
        case _ => Range(0, 1)
      }
      case _ => Range(0, None)
    }
  }

  def strictOn(e : WithContext[Expression]): Set[Var] =  {
    import ImplicitResults._
    e match {
      case Stop() in _ => Set()
      case CallAt(target in _, args, _, ctx) => (target match {
        case Constant(s : Site) => (args collect { case v : Var => v }).toSet // Somehow this is not catching references to some variables
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
      case f < x <| g => f.strictOnSet & g.strictOnSet // TODO: It may be possible to catch a few cases where f is strict on x and so it's strictness can be ignored.
      case LimitAt(f) => f.strictOnSet
      case DeclareDefsAt(defs, defsctx, body) => {
        body.strictOnSet
      }
      case DeclareTypeAt(_, _, b) => b.strictOnSet
      case HasType(b, _) in ctx => this(b)(ctx).strictOnSet
      case Constant(_) in _ => Set()
      case (v : BoundVar) in _ => Set(v)
      case _ => Set()
    }
  }
  
  def effectFree(e : WithContext[Expression]): Boolean = {
    import ImplicitResults._
    e match {
      case Stop() in _ => true
      case CallAt(target, List(Constant(f : Field)), _, _) => { // TODO: Verify the correctness of this.
        target.effectFree
      }
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
}


object Analysis {
  def count(t : Orc5CAST, p : (Expression => Boolean)) : Int = {
    val cs = t.subtrees
    (t match {
      case e : Expression if p(e) => 1
      case _ => 0
    }) +
    (cs.map( count(_, p) ).sum)
  }
  
  val latebindCost = 4
  val sequenceCost = 1
  val otherwiseCost = 2
  val limitCost = 3
  val callCost = 1
  
  def cost(t : Orc5CAST) : Int = {
    val cs = t.subtrees
    (t match {
      case _ : LateBind => latebindCost
      case _ : Sequence => sequenceCost
      case _ : Otherwise => otherwiseCost
      case _ : Limit => limitCost
      case _ : Call => callCost
      case _ => 0
    }) +
    (cs.map( cost(_) ).sum)
  }
}
