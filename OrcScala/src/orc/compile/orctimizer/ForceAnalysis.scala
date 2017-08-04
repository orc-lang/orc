//
// EffectAnalysis.scala -- Scala object and class EffectAnalysis
// Project OrcScala
//
// Created by amp on Jun 1, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import scala.reflect.ClassTag

import orc.ast.orctimizer.named._
import orc.compile.AnalysisRunner
import orc.compile.AnalysisCache
import orc.compile.flowanalysis.LatticeValue
import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.Analyzer
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import FlowGraph.{ Node, Edge }
import orc.util.DotUtils.DotAttributes
import orc.compile.Logger

class ForceAnalysis(
  val results: Map[Expression.Z, Set[BoundVar]],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._
  import EffectAnalysis._

  def edges = graph.edges
  def nodes = graph.nodes
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  def apply(e: Expression.Z): Set[BoundVar] = {
    results.getOrElse(e, Set())
  }

}

object ForceAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), ForceAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): ForceAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val effects = cache.get(EffectAnalysis)(params)
    val delay = cache.get(DelayAnalysis)(params)
    val a = new DelayAnalyzer(cg, effects, delay)
    val res = a()

    new ForceAnalysis(res collect { case (EntryNode(e), r) => (e, r) }, cg)
  }

  val worstState: DelayAnalyzer#StateT = Set()

  class DelayAnalyzer(graph: CallGraph, effects: EffectAnalysis, delay: DelayAnalysis) extends Analyzer {
    import graph._
    type NodeT = Node
    type EdgeT = Edge
    type StateT = Set[BoundVar]

    def initialNodes: collection.Seq[Node] = {
      Seq(graph.exit)
    }

    val initialState = Set()

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.to))
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.from))
    }

    def transfer(node: Node, old: StateT, states: States): (StateT, Seq[Node]) = {
      lazy val inState = states.inStateReduced[Edge](_ intersect _)

      // TODO: This can probably be much tighter. Notably the delay and effect analysis are not actually the correct info since they are for whole expressions.
      val killAll = node match {
        case node @ ExitNode(ast) =>
          effects.effects(ast) ||
            (delay.delayOf(ast).maxFirstPubDelay > ComputationDelay()) ||
            (delay.delayOf(ast).maxHaltDelay > ComputationDelay())
        case node @ EntryNode(ast) =>
          effects.effects(ast) ||
            (delay.delayOf(ast).maxFirstPubDelay > ComputationDelay()) ||
            (delay.delayOf(ast).maxHaltDelay > ComputationDelay())
        case _ =>
          false
      }

      lazy val gen: StateT = node match {
        case node @ EntryNode(ast) =>
          import orc.ast.orctimizer.named._
          ast match {
            // TODO: How to we propagate this information through value edges? This is important to allow the information to propagate through branch and even objects.
            case Force.Z(_, vs, _) =>
              vs.collect({
                case x: BoundVar.Z =>
                  x.value
              }).toSet
            case Resolve.Z(vs, _) =>
              vs.collect({
                case x: BoundVar.Z =>
                  x.value
              }).toSet
            case _ =>
              Set()
          }
        case _ =>
          Set()
      }

      val state: Set[BoundVar] = if (killAll) {
        gen
      } else {
        inState ++ gen
      }

      //Logger.fine(s"$node: $killAll $gen state $state")

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = b subsetOf a
  }
}

