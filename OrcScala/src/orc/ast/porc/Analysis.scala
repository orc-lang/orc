//
// Analysis.scala -- Scala class/trait/object Analysis
// Project OrcScala
//
// Created by amp on Jun 2, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.porc

import scala.collection.mutable
import orc.values.Field
import orc.values.sites.{ Site => OrcSite }
import orc.values.sites.SiteMetadata
import orc.values.sites.Delay

case class AnalysisResults(
  isNotFuture: Boolean,
  doesNotThrowHalt: Boolean,
  fastTerminating: Boolean,
  siteMetadata: Option[SiteMetadata]) {
}

sealed trait AnalysisProvider[E <: PorcAST] {
  outer =>
  def apply(e: PorcAST.Z): AnalysisResults
  def get(e: PorcAST.Z): Option[AnalysisResults]

  object ImplicitResults {
    import scala.language.implicitConversions
    implicit def expressionCtxWithResults(e: PorcAST.Z): AnalysisResults = apply(e)
  }

  def withDefault: AnalysisProvider[E] = {
    new AnalysisProvider[E] {
      def apply(e: PorcAST.Z): AnalysisResults = get(e).getOrElse(AnalysisResults(false, false, false, None))
      def get(e: PorcAST.Z): Option[AnalysisResults] = outer.get(e)
    }
  }
}

/** A cache for storing all the results of a bunch of expressions.
  */
class Analyzer extends AnalysisProvider[PorcAST] {
  val cache = mutable.Map[PorcAST.Z, AnalysisResults]()

  // TODO: Somehow this is running the system out of memory on some programs. For example, /OrcExamples/OrcSites/simanim/baboon.orc

  def apply(e: PorcAST.Z) = {
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
  def get(e: PorcAST.Z) = Some(apply(e))

  def analyze(e: PorcAST.Z): AnalysisResults = {
    AnalysisResults(nonFuture(e), nonHalt(e), fastTerminating(e), siteMetadata(e))
  }

  def translateArguments(vs: List[Argument], formals: List[Variable], s: Set[Variable]): Set[Variable] = {
    val m = (formals zip vs).toMap
    s.collect(m).collect { case v: Variable => v }
  }

  def nonFuture(e: PorcAST.Z): Boolean = {
    ???
  }

  def siteMetadata(e: PorcAST.Z): Option[SiteMetadata] = {
    ???
  }

  def nonHalt(e: PorcAST.Z): Boolean = {
    ???
  }

  // TODO: detect more fast cases for defcall and call. This is important for eliminating spawns at def calls when they are not needed.
  def fastTerminating(e: PorcAST.Z): Boolean = {
    ???
  }
}

object Analysis {
  val closureCost = 5
  val spawnCost = 4
  val forceCost = 3
  val killCost = 2
  val callkillhandlersCost = 5
  val callCost = 1
  val externalCallCost = 5
  val atomicOperation = 2

  def cost(e: PorcAST): Int = {
    val cs = e.subtrees.asInstanceOf[Iterable[PorcAST]]
    (e match {
      case _: Spawn => spawnCost
      case _: Force => forceCost
      case _: Kill => killCost
      case _: Continuation | _: MethodCPS | _: MethodDirect => closureCost
      case _: NewTerminator => closureCost
      case _: CallContinuation => callCost
      case _: MethodDirectCall => externalCallCost
      case _: MethodCPSCall => externalCallCost + spawnCost
      case _: Kill | _: Halt => atomicOperation
      case _ => 0
    }) + (cs.map(cost).sum)
  }

  def count(t: PorcAST, p: (Expression => Boolean)): Int = {
    val cs = t.subtrees.asInstanceOf[Iterable[PorcAST]]
    (t match {
      case e: Expression if p(e) => 1
      case _ => 0
    }) +
      (cs.map(count(_, p)).sum)
  }
}
