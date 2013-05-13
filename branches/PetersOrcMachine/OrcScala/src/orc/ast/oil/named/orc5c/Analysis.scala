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

  /**
   * Is this expression strict on the variable <code>x</code>?
   * In otherwords does it dereference it before doing anything else?
   */
  def strictOn(x : Var): Boolean
  
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
  def strictOn(x : Var) = strictOnSet(x)
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
  def addClosure(x : BoundVar, ctx : AnalysisContext, c : Def) = this.copy(closures = closures.updated(x, (ctx, c)))
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
  var results : Map[Expression, AnalysisResults] = Map()  
  
  def apply(e : Expression) = results.get(e) getOrElse AnalysisResultsConcrete(false, false, (0, None), Set(), false)
  
  //import scala.language.implicitConversions
  //implicit def expressionWithResults(e : Expression) : AnalysisResults = results(e) 
  
  val SeqBoundVarResults = AnalysisResultsConcrete(true, true, (1, Some(1)), Set(), true)
  
  def analyze(e : Expression) {
    analyze(e, AnalysisContext(), (e, r) => results += ((e, r)))
  }
  
  def analyze(e : Expression, ctx: AnalysisContext, resHandler : (Expression, AnalysisResults) => Unit) : AnalysisResults = {
    def res(r : AnalysisResults) = { resHandler(e, r); r }
    def a(es : Expression*) : AnalysisContext = as(es)
    def as(es : Seq[Expression]) : AnalysisContext = ctx.addExpressions(es.map(e => (e, analyze(e, ctx, resHandler))))
    def recurse(e : Expression) = res(analyze(e, ctx, resHandler))
    def compute(e : Expression, ctx : AnalysisContext) = 
      res(AnalysisResultsConcrete(immediateHalt(e, ctx), immediatePublish(e, ctx), publications(e, ctx), strictOn(e, ctx), effectFree(e, ctx)))
    
    e match {
      case Stop() => res(AnalysisResultsConcrete(true, false, (0, Some(0)), Set(), true))
      case f || g => compute(e, a(f, g))
      case f ow g => compute(e, a(f, g))
      case f > x > g => {
        val ctx1 = a(f)
        val ctx2 = ctx1.addExpressions(Seq((g, analyze(g, ctx.addVariable(x, SeqBoundVarResults), resHandler))))
        resHandler(x, SeqBoundVarResults)
        compute(e, ctx2)
      }
      case f < x <| g => {
        val ctx1 = a(g)
        val varResults = AnalysisResultsConcrete(
            ctx1(g).immediatePublish, 
            ctx1(g).immediatePublish || ctx1(g).immediateHalt, 
            (ctx1(g).publications._1 min 1, ctx1(g).publications._2 map (_ min 1)), 
            Set(), 
            true)
        val ctx2 = ctx1.addVariable(x, varResults)
        val ctx3 = ctx2.addExpressions(Seq((f, analyze(f, ctx2, resHandler))))
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
        val ctx1 = defs.foldLeft(ctx)((c, d) => c.addClosure(d.name, ctx, d)) // FIXME: This context needs to be generated recursively. :-(
        for(Def(name, formals, body, _, _, _) <- defs) {
          analyze(body, ctx1, resHandler)
        }
        res(analyze(body, ctx1, resHandler))
      }
      case DeclareType(_, _, b) => recurse(b)
      case HasType(b, _) => recurse(b)
      case _ => {
        assert(e.subtrees.isEmpty)
        compute(e, ctx)
      }
    }    
  }
  
  def analyzeClosureCall(ctx: AnalysisContext, x : BoundVar, args : Seq[AnalysisResults]) : Option[AnalysisResults] = {
    ctx.getClosure(x) map { p => 
      val (ctx, d) = p
      analyze(d.body, ctx.addVariables(d.formals zip args), (_,_) => ())
    }
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
        case Constant(s : Site) => s.immediateHalt
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
                         // TODO: analyze(f, ctx.addVariable(x, immediatepublish)).immediatePublish && g.immediatePublish
      case Limit(f) => f.immediatePublish
      case Call(target, args, _) => {
        val targetIP = (target match {
          case Constant(s: Site) => s.immediatePublish
          case v: BoundVar =>
            (analyzeClosureCall(ctx, v, args.map(ctx(_))) map (_.immediatePublish) getOrElse false) &&
              (ctx.get(v) map (_.immediatePublish) getOrElse false)
          case _ => false
        })
        val argsIP = args.forall(a => { /*println(s"$a ${a.immediatePublish}");*/ a.immediatePublish }) // FIXME: Is this right?
        /*println(ctx.expressions)
        println(s"$e $targetIP $argsIP")
        */
        targetIP && argsIP 
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
        case _ => (0, None)
      })
      case Limit(f) => (f.publications._1 min 1, Some(f.publications._2 map (_ min 1) getOrElse 1))
      case f || g => (f.publications._1 + g.publications._1, 
          (f.publications._2, g.publications._2) match {
        case (Some(n), Some(m)) => Some(n+m)
        case _ => None
          })
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
        case Constant(s : Site) => Set()
        case v : BoundVar => Set(v)
      }) ++ args collect { case v : BoundVar => v }
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
        case _ => false
      })
      case f || g => f.effectFree && g.effectFree
      case f ow g => (f.effectFree && g.effectFree) ||
                     (f.silent && g.effectFree) ||
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