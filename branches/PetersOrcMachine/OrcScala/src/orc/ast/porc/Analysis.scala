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
    nonFuture: Boolean
    ) {
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
      def apply(e: WithContext[E]) : AnalysisResults = get(e).getOrElse(AnalysisResults(false))
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
    AnalysisResults(nonFuture(e))
  }
  
  def translateArguments(vs: List[Value], formals: List[Var], s: Set[Var]): Set[Var] = {
    val m = (formals zip vs).toMap
    s.collect(m).collect { case v: Var => v }
  }
  def nonFuture(e : WithContext[PorcAST]): Boolean = {
    import ImplicitResults._
    /*e match {
      case (_:ClosureVariable) in _ => true
      case (_:SiteVariable) in _ => true
      case (v:Variable) in ctx if ctx(v).isInstanceOf[LetArgumentBound] => true
      case (_:Constant) in _ => true
      case Tuple(vs) in ctx => vs.forall(v => (v in ctx).nonFuture)
      case _ => false
    }*/
    // This needs types.
    false 
  }
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
      case _ : SetKill => killCost
      case _ : CallKillHandlers => callkillhandlersCost
      case _ : Let | _ : Site => closureCost
      case _ : Call | _ : SiteCall => callCost
      case _ : ExternalCall => externalCallCost
      case _ => 0
    }) +
    (cs.map( cost(_) ).sum)
  }

}