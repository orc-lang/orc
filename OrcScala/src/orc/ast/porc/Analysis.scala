//
// Analysis.scala -- Scala class/trait/object Analysis
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import scala.collection.mutable
import orc.values.Field
import orc.values.sites.{Site => OrcSite}

case class AnalysisResults(
    isNotFuture: Boolean,
    doesNotThrowHalt: Boolean,
    cost: Int,
    fastTerminating: Boolean
    ) {
}

sealed trait AnalysisProvider[E <: PorcAST] {
  outer =>
  def apply(e: WithContext[E]) : AnalysisResults
  def get(e: WithContext[E]) : Option[AnalysisResults]
  
  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionCtxWithResults(e : WithContext[E]): AnalysisResults = apply(e) 
  }
  
  def withDefault : AnalysisProvider[E] = {
    new AnalysisProvider[E] {
      def apply(e: WithContext[E]) : AnalysisResults = get(e).getOrElse(AnalysisResults(false, false, Int.MaxValue, false))
      def get(e: WithContext[E]) : Option[AnalysisResults] = outer.get(e)
    }
  }
}

/**
 * A cache for storing all the results of a bunch of expressions.
 */
class Analyzer extends AnalysisProvider[PorcAST] {
  val cache = mutable.Map[WithContext[PorcAST], AnalysisResults]()
  def apply(e : WithContext[PorcAST]) = {
    cache.get(e) match {
      case Some(r) => {
        r
      }
      case None => {
        val r = analyze(e)
        cache += e -> r 
        r
      }
    }
  }
  def get(e : WithContext[PorcAST]) = Some(apply(e))
  
  
  def analyze(e : WithContext[PorcAST]) : AnalysisResults = {
    AnalysisResults(nonFuture(e), false, Int.MaxValue, false) // nonHalt(e), cost(e), fastTerminating(e))
  }
  
  def translateArguments(vs: List[Value], formals: List[Var], s: Set[Var]): Set[Var] = {
    val m = (formals zip vs).toMap
    s.collect(m).collect { case v: Var => v }
  }
  def nonFuture(e : WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    e match {
      case (_:OrcValue | _:Unit) in _ => true
      case (v:Var) in ctx  => ctx(v) match {
        case _ : ContinuationArgumentBound | _ : SiteBound | _ : RecursiveSiteBound => true
        case _ => false // This needs types.
      }
      case _ => false
    }
  }
  
  /*
  def nonHalt(e : WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    // TODO: Fill this out to be a more accurate analysis. It should be a type analysis.
    e match {
      case (_:OrcValue | _:Bool | _:Unit | _:Var) in _ => true
      case LambdaIn(_, _, _) => true
      
      case CallIn((t: Var) in ctx, _, _) => ctx(t) match {
        case LetBound(dctx, l) => 
          val LetIn(_, LambdaIn(_, _, b), _) = l in dctx
          b.doesNotThrowHalt
        case _ => false
      }
      
      case IfIn(p, t, e) => t.doesNotThrowHalt && e.doesNotThrowHalt
      case TryOnHaltedIn(b, h) => h.doesNotThrowHalt
      case TryOnKilledIn(b, h) => b.doesNotThrowHalt && h.doesNotThrowHalt
      case SequenceIn(l, ctx) => l forall (e => (e in ctx).doesNotThrowHalt)
      case LetIn(_, v, b) => v.doesNotThrowHalt && b.doesNotThrowHalt
      case KillIn(b, a) => b.doesNotThrowHalt && a.doesNotThrowHalt
      case RestoreCounterIn(b, a) => b.doesNotThrowHalt && a.doesNotThrowHalt
      case NewCounterIn(b) => b.doesNotThrowHalt
      case NewTerminatorIn(b) => b.doesNotThrowHalt
      case (NewFlag() | NewFuture()) in _ => true
      case _ => false
    }
  }
  
  def fastTerminating(e : WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    e match {
      case DirectSiteCallIn(OrcValue(t:OrcSite) in _, _, _) if !t.immediateHalt => false
      case SiteCallIn((t: Var) in ctx, _, _, _) => ctx(t) match {
        case RecursiveSiteBound(_, _, _) => false
        case _ => true
      }
      case KillIn(b, a) => false

      case CallIn((t: Var) in ctx, _, _) => ctx(t) match {
        case LetBound(dctx, l) => 
          val LetIn(_, LambdaIn(_, _, b), _) = l in dctx
          b.fastTerminating
        case _ => false
      }
      
      case IfIn(p, t, e) => t.fastTerminating && e.fastTerminating
      case TryOnHaltedIn(b, h) => b.fastTerminating && h.fastTerminating
      case TryOnKilledIn(b, h) => b.fastTerminating && h.fastTerminating
      case LetIn(_, v, b) => v.fastTerminating && b.fastTerminating
      case SequenceIn(l, ctx) => l forall (e => (e in ctx).fastTerminating)
      case RestoreCounterIn(b, a) => b.fastTerminating && a.fastTerminating
      case NewCounterIn(b) => b.fastTerminating
      case NewTerminatorIn(b) => b.fastTerminating
      case _ => true
    }
  }
  
  val closureCost = 5
  val spawnCost = 4
  val forceCost = 3
  val killCost = 2
  val callkillhandlersCost = 5
  val callCost = 1
  val externalCallCost = 5
  val atomicOperation = 2
  
  def cost(e : WithContext[PorcAST]): Int = {
    import ImplicitResults._
    
    val cs = e.subtrees
    (e.e match {
      case _ : Spawn => spawnCost
      case _ : Force | _ : Resolve => forceCost
      case _ : Kill => killCost
      case _ : Lambda | _ : Site => closureCost
      case _ : Call => callCost
      case _ : ExternalCall | _ : SiteCall => externalCallCost
      case _ : RestoreCounter | _ : CheckKilled | _ : CallCounterHalt | 
        _: DecrCounter | _:MakeCounterTopLevel | _: IsKilled =>
        atomicOperation
      case _ => 0
    }) + (cs.map( _.cost ).sum)
  }
  */
}


object Analysis {
  def count(t : PorcAST, p : (Expr => Boolean)) : Int = {
    val cs = t.subtrees.asInstanceOf[Iterable[PorcAST]]
    (t match {
      case e : Expr if p(e) => 1
      case _ => 0
    }) +
    (cs.map( count(_, p) ).sum)
  }
}