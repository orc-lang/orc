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
  def publications: (Int, Option[Int]) 
  
  def silent = publications._2 == Some(0)
  def talkative = publications._1 >= 1
  
  def publishesAtMost(n : Int) = publications._2 map { i => i <= n } getOrElse false
  def publishesAtLeast(n : Int) = publications._1 >= n

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
}

case class AnalysisResultsConcrete(
  immediateHalt: Boolean, 
  immediatePublish: Boolean,
  publications: (Int, Option[Int]), 
  strictOnSet: Set[Var],
  effectFree: Boolean
    ) extends AnalysisResults {
  if( immediatePublish && !publishesAtLeast(1) )
    assert(publishesAtLeast(1), "immediatePublish must imply at least one publication") 
  if( immediateHalt && publishesAtLeast(1) && !immediatePublish)
    assert(immediatePublish, "immediateHalt and at least 1 publication must imply immediatePublish") 
}

trait ExpressionAnalysisStore {
  outer =>
  def apply(e: Expression) : AnalysisResults
  
  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionWithResults(e : Expression) : AnalysisResults = outer.apply(e) 
  }
}

/**
 * The context of an expression needed for analysis. The contents are only things we know about. There 
 * may be other variables in context that we have no information about.
 * 
 * @param variables A map of variables to analysis information about the properties of dereferencing this variable.
 * @param closures A map of variables to the closures they reference. 
 */
case class AnalysisContext(
    //variables : Map[BoundVar, AnalysisResults] = Map(), 
    closures : Map[BoundVar, (AnalysisContext, Def)] = Map(),
    expressions : Map[Expression, AnalysisResults] = Map()
    ) extends ExpressionAnalysisStore {
  def addVariable(x : BoundVar, a : AnalysisResults) = addVariables(Seq((x, a)))
  def addVariables(vs : Seq[(BoundVar,AnalysisResults)]) = addExpressions(vs)
  def addClosure(x : BoundVar, ctx : AnalysisContext, c : Def) = 
    this.copy(closures = closures + ((x, (ctx, c))))
  def addExpressions(es : Seq[(Expression,AnalysisResults)]) = {
    /*for((e, r) <- es if expressions.contains(e)) {
      println(s"$e: ${expressions(e)} => $r")
    }*/
    this.copy(expressions = expressions ++ es)
  }
  
  /**
   * Get the analysis information for a variable or None if it is not in the context.
   */
  //def getVariable(x : BoundVar) : Option[AnalysisResults] = variables.get(x)
  /*
   * Get analysis infomation about the closure when called on arguments with the given
   * analysis.

  def getClosure(x : BoundVar, args : Seq[AnalysisResults]) : Option[AnalysisResults] = {
    closures.get(x) map { p => 
      val (ctx, d) = p
      analyze(d.body, ctx.addVariables(d.formals zip args), (_,_) => ())
    }
  }
  */
  
  def getClosure(x : BoundVar) = closures.get(x)
  def get(x : Expression) = {
    expressions.get(x)
  }
  
  def apply(e: Expression) = expressions(e)
}

/**
  *
  * @author amp
  */
class Analyzer extends ExpressionAnalysisStore {
  val results : mutable.Map[Expression, AnalysisResults] = mutable.Map()  
  
  def apply(e : Expression) = results.get(e) getOrElse AnalysisResultsConcrete(false, false, (0, None), Set(), false)
  
  val SeqBoundVarResults = AnalysisResultsConcrete(true, true, (1, Some(1)), Set(), true)
  
  def analyze(e : Expression) {
    analyze(e, AnalysisContext(), true)
  }
  
  def analyze(e : Expression, ctx: AnalysisContext, storing: Boolean) : AnalysisResults = {
    def resHandler(e: Expression, r: AnalysisResults) = if( storing ) results += ((e, r))
    def res(r : AnalysisResults) = { resHandler(e, r); r }
    def a(es : Expression*) : AnalysisContext = as(es)
    def as(es : Seq[Expression]) : AnalysisContext = ctx.addExpressions(es.map(e => (e, analyze(e, ctx, storing))))
    def recurse(e : Expression) = res(analyze(e, ctx, storing))
    def compute(e : Expression, ctx : AnalysisContext) = 
      res(AnalysisResultsConcrete(immediateHalt(e, ctx), immediatePublish(e, ctx), publications(e, ctx), strictOn(e, ctx), effectFree(e, ctx)))
    val ret = e match {
      case Stop() => res(AnalysisResultsConcrete(true, false, (0, Some(0)), Set(), true))
      case f || g => compute(e, a(f, g))
      case f ow g => compute(e, a(f, g))
      case f > x > g => {
        val ctx1 = a(f)
        val ctx2 = ctx1.addExpressions(Seq((g, analyze(g, ctx.addVariable(x, SeqBoundVarResults), storing))))
        resHandler(x, SeqBoundVarResults)
        compute(e, ctx2)
      }
      case f < x <| g => {
        val ctx1 = a(g)
        val varResults = AnalysisResultsConcrete(
            ctx1(g).immediatePublish || ctx1(g).immediateHalt,
            ctx1(g).immediatePublish,
            (ctx1(g).publications._1 min 1, Some(ctx1(g).publications._2 map (_ min 1) getOrElse 1)), 
            Set(), 
            true)
        val ctx2 = ctx1.addVariable(x, varResults)
        val ctx3 = ctx2.addExpressions(Seq((f, analyze(f, ctx2, storing))))
        resHandler(x, varResults)
        compute(e, ctx3)
      }
      case Limit(f) => {
        compute(e, a(f))
      }
      case Call(target, args, _) => {
        compute(e, as(target :: args))
      }
      case DeclareDefs(defs, body) => {
        val ctx1 = defs.foldLeft(ctx)((c, d) => c.addClosure(d.name, ctx, d)) 
        val ctx2 = defs.foldLeft(ctx1)((c, d) => c.addVariable(d.name, SeqBoundVarResults))
        // FIXME: This context needs to be generated recursively. :-(
        if(storing) {
          for(Def(name, formals, body, _, _, _) <- defs) {
            resHandler(name, SeqBoundVarResults)
            analyze(body, ctx2, storing)
          }
        }
       res(analyze(body, ctx2, storing))
      }
      case DeclareType(_, _, b) => recurse(b)
      case HasType(b, _) => recurse(b)
      case _ => {
        assert(e.subtrees.isEmpty)
        compute(e, ctx)
      }
    }
    ret
  }
  
  def analyzeClosureCall(ctx: AnalysisContext, x : BoundVar, args : Seq[AnalysisResults]) : Option[AnalysisResults] = {
    /* This is disabled because it makes the analysis so dreadfuly slow. It will be replaced.
     ctx.getClosure(x) map { p => 
      val (ctx, d) = p
      analyze(d.body, ctx.addVariables(d.formals zip args), false)
    }*/
    None
  }
  
  def immediateHalt(e : Expression, ctx: AnalysisContext): Boolean = {
    import ctx.ImplicitResults._
    e match {
      case Stop() => true
      case f || g => f.immediateHalt && g.immediateHalt
      case f ow g => (f.immediateHalt && g.immediateHalt) || 
                     (f.immediateHalt && f.publications._1 > 0)
      case f > x > g => (f.immediateHalt && g.immediateHalt) || 
                        (f.immediateHalt && f.silent)
      case f < x <| g => (f.immediateHalt && g.immediateHalt)
      case Limit(f) => f.immediateHalt || f.immediatePublish
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => s.immediateHalt || args.exists(a => a.immediateHalt && a.silent)
        case v : BoundVar => 
          (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (_.immediateHalt) getOrElse false) || 
          (ctx.get(v) map (r => r.immediateHalt && r.silent) getOrElse false) 
        case _ => false
      }) && args.forall(a => a.immediatePublish || a.immediateHalt) // FIXME: Is this right?
      case DeclareDefs(defs, body) => {
        body.immediateHalt
      }
      case DeclareType(_, _, b) => b.immediateHalt
      case HasType(b, _) => b.immediateHalt
      case Constant(_) => true
      case v : BoundVar => ctx.get(v) map {_.immediateHalt} getOrElse false
      case _ => false
    }
  }
  def immediatePublish(e : Expression, ctx: AnalysisContext): Boolean = {
    import ctx.ImplicitResults._
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
          case v: BoundVar =>
            (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (_.immediatePublish) getOrElse false)
          case _ => false
        }         
      case DeclareDefs(defs, body) => {
        body.immediateHalt
      }
      case DeclareType(_, _, b) => b.immediatePublish
      case HasType(b, _) => b.immediatePublish
      case Constant(_) => true
      case v : BoundVar => ctx.get(v) map (_.immediatePublish) getOrElse false
      case _ => false
    }
  }
 
  def publications(e : Expression, ctx: AnalysisContext): (Int, Option[Int]) = {
    import ctx.ImplicitResults._

    def comp(minF: (Int, Int) => Int, maxF: (Int, Int) => Int)(f: AnalysisResults, g: AnalysisResults) = {
      (minF(f.publications._1, g.publications._1),
        (f.publications._2, g.publications._2) match {
          case (Some(n), Some(m)) => Some(maxF(n, m))
          case _ => None
        })
    }

    e match {
      case Stop() => (0, Some(0))
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => {
          if( args.forall(a => a.talkative) )
            s.publications
            else
            (0, s.publications._2)
            // TODO: Could also detect silent arguments and guarentee this is silent as well.
        }
        case v: BoundVar =>
          (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (comp(_ min _, (n, m) => (if(n == 0) 0 else m))(_,v)) getOrElse (0, None))  
               
        case _ => (0, None)
      })
      case Limit(f) => (f.publications._1 min 1, Some(f.publications._2 map (_ min 1) getOrElse 1))
      case f || g =>
        comp(_ + _, _ + _)(f, g)
      case f ow g => 
        comp((n, m) => (if(n == 0) 1 else n) min m, _ max _)(f, g)
      case f > x > g =>
        comp(_ * _, _ * _)(f, g)
      case f < x <| g => f.publications
      case DeclareType(_, _, b) => b.publications
      case HasType(b, _) => b.publications
      case Constant(_) => (1, Some(1))
      case v : BoundVar => ctx.get(v) map (_.publications) getOrElse (0, Some(1))
      case _ => (0, None)
    }
  }

  def strictOn(e : Expression, ctx: AnalysisContext): Set[Var] =  {
    import ctx.ImplicitResults._
    e match {
      case Stop() => Set()
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => (args collect { case v : Var => v }).toSet
        case v: BoundVar =>
            (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (_.strictOnSet) getOrElse Set()) ++ Set(v) 
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
  
  def effectFree(e : Expression, ctx: AnalysisContext): Boolean = {
    import ctx.ImplicitResults._
    e match {
      case Stop() => true
      case Call(target, args, _) => (target match {
        case Constant(s : Site) => s.effectFree
        case v: BoundVar =>
            (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (_.effectFree) getOrElse false)
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

object Analysis {
  def count(t : Orc5CAST, p : (Expression => Boolean)) : Int = {
    val cs = t.subtrees
    (t match {
      case e : Expression if p(e) => 1
      case _ => 0
    }) +
    (cs.map( count(_, p) ).sum)
  }
}
