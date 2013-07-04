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

case class AnalysisResults(
    immediatelyCallsSet: Set[Var],
    nonFuture: Boolean
    ) {
  def immediatelyCalls(v : Var) = immediatelyCallsSet(v)
}

sealed trait AnalysisProvider[E <: PorcAST] {
  outer =>
  def apply(e: WithContext[E]) : AnalysisResults
  def get(e: WithContext[E]) : Option[AnalysisResults]
  
  object ImplicitResults {
    import scala.language.implicitConversions
    @inline implicit def expressionCtxWithResults(e : WithContext[E]): AnalysisResults = apply(e) 
  }
  
  def withDefault : AnalysisProvider[E] = {
    new AnalysisProvider[E] {
      def apply(e: WithContext[E]) : AnalysisResults = get(e).getOrElse(AnalysisResults(Set(), false))
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
    AnalysisResults(immediatelyCalls(e), nonFuture(e))
  }
  
  def translateArguments(vs: List[Value], formals: List[Var], s: Set[Var]): Set[Var] = {
    val m = (formals zip vs).toMap
    s.collect(m).collect { case v: Var => v }
  }
  
  def immediatelyCalls(e : WithContext[PorcAST]): Set[Var] = {
    import ImplicitResults._
    e match {
      case LetIn(d, b) => b.immediatelyCallsSet
      case SiteIn(l, ctx, b) => b.immediatelyCallsSet

      case ClosureCallIn((v : Var) in ctx, a, _) => ctx(v) match {
        case LetBound(ctx, n) => (n.d in ctx) match {
          case ClosureDefIn(_, formals, _, body) => translateArguments(a, formals, body.immediatelyCallsSet) + v
        }
        case _ => Set(v)
      }
      case SiteCallIn((v: Var) in ctx, a, _) => {
        val b = ctx(v) 
        b match {
          case SiteBound(ctx, _, d) => (d in ctx) match {
            case SiteDefIn(_, formals, _, body) => translateArguments(a, formals, body.immediatelyCallsSet) + v
          }
          case _ => a match {
            case List(Tuple(List(Constant(f : Field))), p, h : Var) => Set(v, h)
            case _ => Set(v)
          }
        }
      }
      
      case UnpackIn(vars, v, k) => k.immediatelyCallsSet
      
      case SpawnIn(v, k) => k.immediatelyCallsSet
        
      case NewCounterIn(k) => k.immediatelyCallsSet
      case RestoreCounterIn(a, b) => a.immediatelyCallsSet & b.immediatelyCallsSet
      case SetCounterHaltIn(v, k) => k.immediatelyCallsSet
      case GetCounterHaltIn(x, k) => k.immediatelyCallsSet

      case NewTerminatorIn(k) => k.immediatelyCallsSet
      case GetTerminatorIn(x, k) => k.immediatelyCallsSet
      case KillIn(a, b) => a.immediatelyCallsSet & b.immediatelyCallsSet
      case IsKilledIn(a, b) => a.immediatelyCallsSet & b.immediatelyCallsSet
      case AddKillHandlerIn(u, m, k) => k.immediatelyCallsSet
      case CallKillHandlersIn(k) => k.immediatelyCallsSet
        
      case NewFutureIn(x, k) => k.immediatelyCallsSet
      case ForceIn(vs, a, b) => Set()
      case BindIn(f, v, k) => k.immediatelyCallsSet
      case StopIn(f, k) => k.immediatelyCallsSet
      
      case NewFlagIn(x, k) => k.immediatelyCallsSet
      case SetFlagIn(f, k) => k.immediatelyCallsSet
      case ReadFlagIn(f, a, b) => a.immediatelyCallsSet & b.immediatelyCallsSet

      case ExternalCallIn(site, _, _, (h:Var) in _) if site.immediateHalt => Set(h)
      //case ExternalCallIn(site, Tuple(List(Constant(f : Field))) in _, _, (h:Var) in _) => Set(h)

      case _ => Set()
    }
  }
  
  def nonFuture(e : WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    e match {
      case (_:ClosureVariable) in _ => true
      case (_:SiteVariable) in _ => true
      case (v:Variable) in ctx if ctx(v).isInstanceOf[LetArgumentBound] => true
      case (_:Constant) in _ => true
      case Tuple(vs) in ctx => vs.forall(v => (v in ctx).nonFuture)
      case _ => false
    }
  }
}


object Analysis {
  def count(t : PorcAST, p : (Command => Boolean)) : Int = {
    val cs = t.subtrees.asInstanceOf[Iterable[PorcAST]]
    (t match {
      case e : Command if p(e) => 1
      case _ => 0
    }) +
    (cs.map( count(_, p) ).sum)
  }
  
  val closureCost = 5
  val spawnCost = 5
  val forceCost = 3
  val killCost = 2
  val callkillhandlersCost = 5
  val callCost = 1
  val externalCallCost = 2
  
  def cost(t : PorcAST) : Int = {
    val cs = t.subtrees.asInstanceOf[Iterable[PorcAST]]
    (t match {
      case _ : Spawn => spawnCost
      case _ : Force => forceCost
      case _ : Kill => killCost
      case _ : CallKillHandlers => callkillhandlersCost
      case _ : Let | _ : Site => closureCost
      case _ : ClosureCall | _ : SiteCall => callCost
      case _ : ExternalCall => externalCallCost
      case _ => 0
    }) +
    (cs.map( cost(_) ).sum)
  }

}