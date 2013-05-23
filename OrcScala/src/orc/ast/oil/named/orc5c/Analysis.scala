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
  def apply(e: E) : AnalysisResults
  
  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionWithResults(e : E) : AnalysisResults = apply(e) 
  }
  
  def toMap : collection.Map[E, AnalysisResults]
  
  def withDefault : ExpressionAnalysisProvider[E] = {
    val m = toMap
    new ExpressionAnalysisProvider[E] {
      def apply(e: E) : AnalysisResults = m.getOrElse(e, AnalysisResultsConcrete(false, false, Range(0, None), Set(), false))
      def toMap = m
    }
  }
}

/*
There are 2 distinct kinds of ExpressionAnalysisProviders. First are contexts which will 
recompute the value based on the context. Second are stores that actually keep information 
about expression in the context of some larger expression.
*/
/**
 * The context in which analysis is occuring. 
 */
class AnalysisContext(val bindings : Map[BoundVar, Binding]) extends ExpressionAnalysisProvider[BoundVar] {
  def apply(e : BoundVar) : AnalysisResults = bindings(e) 
  def toMap = bindings
  
  def this() = this(Map())
  
  def extend(bs : Iterable[Binding]) : AnalysisContext = {
    new AnalysisContext(bindings ++ bs.map(b => (b.variable, b)))
  }
  def ++(bs : Iterable[Binding]) = extend(bs)
  
  def +(b : Binding) : AnalysisContext = {
    new AnalysisContext(bindings + ((b.variable, b)))
  }
}

/**
 * The class representing binding in the context. The analysis results stored in it refer 
 * to a simple direct reference to the variable.
 */
sealed trait Binding extends AnalysisResults {
  /**
   * The variable that is bound.
   */
  val variable : BoundVar

  val effectFree = true
}

case class SeqBound(variable : BoundVar) extends Binding {
  val immediateHalt = true
  val immediatePublish = true
  val publications = Range(1, 1)
  val strictOnSet : Set[Var] = Set(variable)
}

case class LateBound(
  variable: BoundVar,
  immediateHalt: Boolean,
  immediatePublish: Boolean,
  publications: Range) extends Binding {
  val strictOnSet : Set[Var] = Set(variable)
}
object LateBound {
  /**
   * g is the analysis results of the expression producing the value.
   */
  def apply(v : BoundVar, g : AnalysisResults) : LateBound = {
    LateBound(v,
             g.immediatePublish || g.immediateHalt,
             g.immediatePublish,
             g.publications limitTo 1)
  }
}

case class DefBound(variable : BoundVar, d : Def, body : AnalysisResults) extends Binding {
  val immediateHalt = true
  val immediatePublish = true
  val publications = Range(1, 1)
  val strictOnSet : Set[Var] = Set(variable)
 
  def call(args : IndexedSeq[(Argument, AnalysisResults)]) : AnalysisResults = {
    val strictSilent = body.strictOnSet.exists{ v => d.formals.contains(v) && args(d.formals.indexOf(v))._2.silent }
    val strictSilentHalt = body.strictOnSet.exists{ v => 
      d.formals.contains(v) && {
        val a = args(d.formals.indexOf(v))._2
        a.silent && a.immediateHalt 
      }
    }
    AnalysisResultsConcrete(
      body.immediateHalt || strictSilentHalt,
      body.immediatePublish,
      if (strictSilent) Range(0, 0) else body.publications,
      (body.strictOnSet flatMap { v =>
        if (d.formals.contains(v))
          args(d.formals.indexOf(v)) match {
            case (v: BoundVar, _) => Some(v : Var)
            case _ => None
          }
        else
          None
      }) + (variable : Var),
      body.effectFree)
  }
}
object DefBound {
  def apply(variable : BoundVar, d : Def, ctx : AnalysisContext) : DefBound = {
    val bodyCtx = ctx.extend(d.formals.map(UnknownArgumentBound(_))) 
    val body = Analysis.analyzeIn(bodyCtx, d.body)
    DefBound(variable, d, body)
  }
} 

case class UnknownArgumentBound(variable : BoundVar) extends Binding {
  val immediateHalt = false
  val immediatePublish = false
  val publications = Range(0, 1)
  val strictOnSet : Set[Var] = Set(variable)
}
case class RecursiveDefBound(variable : BoundVar) extends Binding {
  val immediateHalt = true
  val immediatePublish = true
  val publications = Range(1, 1)
  val strictOnSet : Set[Var] = Set(variable)
}

/**
 * A cache for storing all the results of a bunch of expressions.
 */
class ExpressionAnalysisCache extends ExpressionAnalysisProvider[Expression] {
  val cache = mutable.Map[Expression, AnalysisResults]()
  def apply(e : Expression) = cache(e)
  
  def +=(e : Expression, r : AnalysisResults) = {
    assert(checkInsert(e, r))
    cache += ((e, r))
  }
  def ++=[E <: Expression](p : ExpressionAnalysisProvider[E]) = {
    val m = p.toMap
    assert(m.forall(p => checkInsert(p._1, p._2)))
    cache ++= m
  }
  
  private def checkInsert(e : Expression, r : AnalysisResults) : Boolean = {
    if( cache.contains(e) ) {
      if(cache(e) same r) 
        true
        else {
          println("Inserted different analysis: " + cache(e) + " " + r)
          false
        }
          
    } else
      true
  }
  
  def toMap = cache
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
  
  
  def fix[A, B, T](f:((A,B)=>T)=>((A,B)=>T)): (A,B)=>T = f((x:A, y:B) => fix(f)(x,y))
  /**
   * Analyze an expression in the given context. This returns only the
   * analysis of the top level expression.
   */
  def analyzeIn(ctx : AnalysisContext, e : Expression) : AnalysisResults = {
    fix(analyze)(ctx, e)
  }
  /**
   * Analyze an expression in the given context. This returns only the
   * analysis of the top level expression.
   */
  def analyzeAllIn(ctx : AnalysisContext, e : Expression) : ExpressionAnalysisProvider[Expression] = {
    val results = new ExpressionAnalysisCache()
    def h(recurse : (AnalysisContext, Expression) => AnalysisResults)(ctx : AnalysisContext, e : Expression) : AnalysisResults = {
      val r = recurse(ctx, e)
      results += (e, r)
      r
    }
    fix(h _ compose analyze)(ctx, e)
    results
  }
  
  def analyzeAll(e: Expression) = analyzeAllIn(new AnalysisContext(), e)
  
  /**
   * 
   */
  def analyze(recurse : (AnalysisContext, Expression) => AnalysisResults)(ctx : AnalysisContext, e : Expression) : AnalysisResults = {
    /*def resHandler(e: Expression, r: AnalysisResults) = if( storing ) results += ((e, r))
    def res(r : AnalysisResults) = { resHandler(e, r); r }
    def a(es : Expression*) : AnalysisContext = as(es)
    def as(es : Seq[Expression]) : AnalysisContext = ctx.addExpressions(es.map(e => (e, analyze(e, ctx, storing))))
    def recurse(e : Expression) = res(analyze(e, ctx, storing))
    def compute(e : Expression, ctx : AnalysisContext) = 
      res(AnalysisResultsConcrete(immediateHalt(e, ctx), immediatePublish(e, ctx), publications(e, ctx), strictOn(e, ctx), effectFree(e, ctx)))
    def compute(e : Expression, ctx : AnalysisContext) = 
      res(AnalysisResultsConcrete(immediateHalt(e, ctx), immediatePublish(e, ctx), publications(e, ctx), strictOn(e, ctx), effectFree(e, ctx)))
    */
    val c = new ExpressionAnalysisCache()
    c ++= ctx
    def a(es : Expression*) {
      as(es)
    }
    def as(es : Seq[Expression]) {
      for(e <- es) {
        c += (e, recurse(ctx, e))
      }
    }
    def compute(e : Expression, ctx : AnalysisContext) : AnalysisResults =
      AnalysisResultsConcrete(immediateHalt(e, c, ctx), immediatePublish(e, c, ctx), publications(e, c, ctx), strictOn(e, c, ctx), effectFree(e, c, ctx))
    
      
    val ret = e match {
      case Stop() => AnalysisResultsConcrete(true, false, Range(0,0), Set(), true)
      case f || g => {
        a(f, g)
        compute(e, ctx)
      }
      case f ow g => {
        a(f, g)
        compute(e, ctx)
      }
      case f > x > g => {
        a(f)
        c += (g, recurse(ctx + SeqBound(x), g))
        recurse(ctx + SeqBound(x), x)
        compute(e, ctx)
      }
      case f < x <| g => {
        a(g)
        c += (f, recurse(ctx + LateBound(x, c(g)), f))
        recurse(ctx + LateBound(x, c(g)), x)
        compute(e, ctx)
      }
      case Limit(f) => {
        a(f)
        compute(e, ctx)
      }
      case Call(target, args, _) => {
        as(target :: args)
        target match {
          case v : BoundVar =>
            ctx(v) match {
              case d@DefBound(_,_,_) =>
                d.call(args.map(a => (a, c(a))).toIndexedSeq)
              case _ => compute(e, ctx)
            }
          case _ => compute(e, ctx)
        }
      }
      case DeclareDefs(defs, body) => {
        val ctx0 = ctx ++ defs.map(d => RecursiveDefBound(d.name))
        val ctx1: AnalysisContext = defs.foldLeft(ctx) { (c, d) =>
          c + DefBound(d.name, d, recurse(c ++ ctx0.toMap.values ++ d.formals.map(UnknownArgumentBound(_)), d.body))
        }
        // FIXME: This context needs to be generated recursively. :-(

        recurse(ctx1, body)
      }
      case DeclareType(_, _, b) => recurse(ctx, b)
      case HasType(b, _) => recurse(ctx, b)
      case _ => {
        assert(e.subtrees.isEmpty)
        compute(e, ctx)
      }
    }
    ret
  }
  
  def immediateHalt(e : Expression, c: ExpressionAnalysisProvider[Expression], ctx : AnalysisContext): Boolean = {
    import c.ImplicitResults._
    e match {
      case Stop() => true
      case f || g => f.immediateHalt && g.immediateHalt
      case f ow g => (f.immediateHalt && g.immediateHalt) || 
                     (f.immediateHalt && f.publications > 0)
      case f > x > g => (f.immediateHalt && g.immediateHalt) || 
                        (f.immediateHalt && f.silent)
      case f < x <| g => (f.immediateHalt && g.immediateHalt)
      case Limit(f) => f.immediateHalt || f.immediatePublish
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => s.immediateHalt || args.exists(a => a.immediateHalt && a.silent)
        case v : BoundVar => v.immediateHalt && v.silent
        case _ => false
      }) && args.forall(a => a.immediatePublish || a.immediateHalt)
      case DeclareDefs(defs, body) => {
        body.immediateHalt
      }
      case DeclareType(_, _, b) => b.immediateHalt
      case HasType(b, _) => b.immediateHalt
      case Constant(_) => true
      case v : BoundVar => ctx(v).immediateHalt
      case _ => false
    }
  }
  def immediatePublish(e : Expression, c: ExpressionAnalysisProvider[Expression], ctx : AnalysisContext): Boolean = {
    import c.ImplicitResults._
    e match {
      case Stop() => false
      case f || g => f.immediatePublish || g.immediatePublish
      case f ow g => (f.immediatePublish) || 
                     (f.immediateHalt && g.immediatePublish)
      case f > x > g => (f.immediatePublish && g.immediatePublish)
      case f < x <| g => f.immediatePublish 
      case Limit(f) => f.immediatePublish
      case Call(target, args, _) => target match {
        case Constant(s: Site) => s.immediatePublish && args.forall(_.immediatePublish)
        case v: BoundVar => false
        case _ => false
      }
      case DeclareDefs(defs, body) => {
        body.immediateHalt
      }
      case DeclareType(_, _, b) => b.immediatePublish
      case HasType(b, _) => b.immediatePublish
      case Constant(_) => true
      case v : BoundVar => ctx(v).immediatePublish
      case _ => false
    }
  }
 
  def publications(e : Expression, c: ExpressionAnalysisProvider[Expression], ctx : AnalysisContext): Range = {
    import c.ImplicitResults._
    e match {
      case Stop() => Range(0,0)
      case Call(target, args, _) => (target match {
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
      case Limit(f) => f.publications.limitTo(1)
      case f || g => f.publications + g.publications
      case f ow g if f.publications.maxi == 0 =>
        Range(1 min g.publications.mini, (f.publications.maxi, g.publications.maxi) match {
          case (Some(n), Some(m)) => Some(n max m)
          case _ => None
        })
      case f ow g if f.publishesAtLeast(1) => f.publications
      case f > x > g => f.publications * g.publications
      case f < x <| g => f.publications
      case DeclareType(_, _, b) => b.publications
      case HasType(b, _) => b.publications
      case Constant(_) => Range(1,1)
      case v : BoundVar => ctx(v).publications
      case _ => Range(0, None)
    }
  }

  def strictOn(e : Expression, c: ExpressionAnalysisProvider[Expression], ctx : AnalysisContext): Set[Var] =  {
    import c.ImplicitResults._
    e match {
      case Stop() => Set()
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => (args collect { case v : Var => v }).toSet
        case v : Var => Set(v)
        case _ => Set()
      })
      case f || g => f.strictOnSet & g.strictOnSet
      case f ow g => f.strictOnSet
      case f > x > g => f.strictOnSet
      case f < x <| g => f.strictOnSet & g.strictOnSet
      case Limit(f) => f.strictOnSet
      case DeclareType(_, _, b) => b.strictOnSet
      case HasType(b, _) => b.strictOnSet
      case Constant(_) => Set()
      case v : BoundVar => Set(v)
      case _ => Set()
    }
  }
  
  def effectFree(e : Expression, c: ExpressionAnalysisProvider[Expression], ctx : AnalysisContext): Boolean = {
    import c.ImplicitResults._
    e match {
      case Stop() => true
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => s.effectFree
        case _ => false
      })
      case f || g => f.effectFree && g.effectFree
      case f ow g => (f.effectFree && g.effectFree) ||
                     (f.talkative && f.effectFree)
      case f > x > g => (f.effectFree && g.effectFree) ||
                        (f.silent && f.effectFree)
      case f < x <| g => f.effectFree && g.effectFree
      case Limit(f) => f.effectFree
      case DeclareType(_, _, b) => b.effectFree
      case HasType(b, _) => b.effectFree
      case Constant(_) => true
      case v : BoundVar => true
      case _ => false
    }
  }
}
