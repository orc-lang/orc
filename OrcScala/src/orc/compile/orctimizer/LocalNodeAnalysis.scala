//
// LocalNodeAnalysis.scala -- Scala object and class LocalNodeAnalysis
// Project OrcScala
//
// Created by amp on Sept 8, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.compile.AnalysisRunner
import orc.compile.AnalysisCache
import orc.ast.orctimizer.named._
import FlowGraph._
import orc.values.sites.Effects

class LocalNodeAnalysis(graph: CallGraph) {
  import graph.NodeInformation._
    
  def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
    (a, b) match {
      case (Some(a), Some(b)) => Some(f(a, b))
      case (None, Some(b)) => Some(b)
      case (Some(a), None) => Some(a)
      case (None, None) => None
    }
  
  def effected(node: Node): Boolean = {
    node match {
      case node: TokenFlowNode  =>
        node.location match {
          // Only calls can observe effects.
          case Call.Z(target, args, _) => {
            val (extPubs, intPubs, otherPubs) = target.byCallTargetCases(
              externals = { vs =>
                // FIXME: Update to use compile time "invoker" API once available. This will avoid problems of too specific results since the Site assumes a specific arity, etc.
                vs.collect({
                  case site: orc.values.sites.SpecificArity =>
                    args.size == site.arity
                  case _ =>
                    true
                }).reduce(_ || _)
              }, internals = { vs =>
                false
              }, others = { vs =>
                true
              })
            applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ || _))(_ || _).getOrElse(true)
          }
          case _ =>
            false
        }
      case EverywhereNode =>
        true
      case _: ValueNode =>
        false
    }
  }
  
  def effects(node: Node): Boolean = {
    node match {
      case node: TokenFlowNode  =>
        node.location match {
          // Only calls can have effects.
          case Call.Z(target, args, _) => {
            val (extPubs, intPubs, otherPubs) = target.byCallTargetCases(
              externals = { vs =>
                // FIXME: Update to use compile time "invoker" API once available. This will avoid problems of too specific results since the Site assumes a specific arity, etc.
                vs.collect({
                  case site: orc.values.sites.SpecificArity =>
                    if (args.size == site.arity) {
                      site.effects != Effects.None
                    } else {
                      false
                    }
                  case site: orc.values.sites.Site => 
                    site.effects != Effects.None
                  case _ =>
                    true
                }).reduce(_ || _)
              }, internals = { vs =>
                false
              }, others = { vs =>
                true
              })
            applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ || _))(_ || _).getOrElse(true)
          }
          case _ =>
            false
        }
      case EverywhereNode =>
        true
      case _: ValueNode =>
        false
    }
  }
}

/** A simple "analysis" which just provides local information about nodes in the graph.
  *
  * This is not really an analysis and just computes it's values as needed. It uses
  * the CallGraph internally.
  *
  */
object LocalNodeAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), LocalNodeAnalysis] {
  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): LocalNodeAnalysis = {
    val cg = cache.get(CallGraph)(params)
    new LocalNodeAnalysis(cg)
  }
}
