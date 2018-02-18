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

import orc.ast.orctimizer.named._
import orc.compile.AnalysisRunner
import orc.compile.AnalysisCache
import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.{Analyzer, AnalyzerEdgeCache}
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import FlowGraph.{ Node, Edge }
import orc.util.DotUtils.DotAttributes
import orc.compile.orctimizer.FlowGraph.TokenFlowNode
import orc.compile.orctimizer.FlowGraph.EntryExitEdge

class ForceAnalysis(
  val results: Map[Expression.Z, Set[BoundVar]],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._

  val edges = graph.edges.filter({ case e: HappensBeforeEdge => true; case _ => false })
  val nodes = edges.flatMap(e => Set(e.from, e.to))
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  def apply(e: Expression.Z): Set[BoundVar] = {
    results.getOrElse(e, Set())
  }

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (n match {
      case EntryNode(n) =>
        results.get(n)
      case _ =>
        None
    }) match {
      case Some(s) =>
        Map("label" -> s"${n.label}\n${s.mkString(", ")}")
      case None => Map()
    }
  }
}

object ForceAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), ForceAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): ForceAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val effects = cache.get(EffectAnalysis)(params)
    val delay = cache.get(DelayAnalysis)(params)
    val local = cache.get(LocalNodeAnalysis)(params)
    val initial = cg.nodes.collect({ case VariableNode(x, _) => x }).toSet
    val a = new DelayAnalyzer(initial, cg, effects, delay, local)
    val res = a()
    

    val r = new ForceAnalysis(res collect { case (EntryNode(e), r) if r.nonEmpty => (e, r) }, cg)
    
    /*
    println("=============== force results ---")
    def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
    println(r.results.par.map(p => s"${shortString(p._1.value)}\t----=========--> ${p._2}").seq.mkString("\n"))
		*/
    //r.debugShow()

    r
  }

  val worstState: DelayAnalyzer#StateT = Set()

  class DelayAnalyzer(val initialState: Set[BoundVar], graph: CallGraph, effects: EffectAnalysis, delay: DelayAnalysis, local: LocalNodeAnalysis) extends Analyzer with AnalyzerEdgeCache {
    import graph._
    import graph.NodeInformation._
    import delay.NodeInformation._
    
    type NodeT = Node
    type EdgeT = Edge
    type StateT = Set[BoundVar]

    def initialNodes: collection.Seq[Node] = {
      nodesBy({
        case n: TokenFlowNode if inputs(n).isEmpty =>
          n
      }).toSeq
    }

    def edgePredicate(e: Edge): Boolean = {
      e match {
        case EntryExitEdge(ast: Call.Z) =>
          val (a, b, c) = ast.target.byCallTargetCases(_ => true, _ => false, _ => true)
          // It's not clear how a call with an unassigned target can reach here, however it is safe to include the edge.
          Seq(a, b, c).flatten.reduceOption(_ || _).getOrElse(true)
        case EntryExitEdge(ast) => false
        case CombinatorInternalOrderEdge(from, _, ExitNode(Branch.Z(_, _, _))) => false
        case _ => true
      }
    }
    
    def inputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdgesOf[HappensBeforeEdge].filter(edgePredicate).toSeq.map(e => ConnectedNode(e, e.to))
    }

    def outputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdgesOf[HappensBeforeEdge].filter(edgePredicate).toSeq.map(e => ConnectedNode(e, e.from))
    }

    def transfer(node: Node, old: StateT, states: States): (StateT, Seq[Node]) = {
      lazy val inState = states.inStateReduced[Edge](_ intersect _)

      val killAll = node match {
        case n: ExitNode if inputs(n).isEmpty =>
          // The initial nodes all have empty output. Unless they gen of course.
          true
        case node: TokenFlowNode =>
          local.effects(node) || 
            (node.delay.maxFirstPubDelay > ComputationDelay()) ||
            (node.delay.maxHaltDelay > ComputationDelay())
          /*
          effects.effects(ast) ||
            (delay.delayOf(ast).maxFirstPubDelay > ComputationDelay()) ||
            (delay.delayOf(ast).maxHaltDelay > ComputationDelay())
        case node @ EntryNode(ast) =>
          effects.effects(ast)
        case _ =>
          false
          */
      }

      lazy val gen: StateT = node match {
        case node @ EntryNode(ast) =>
          import orc.ast.orctimizer.named._
          ast match {
            // TODO: How do we propagate this information through value edges? This is important to allow the information to propagate through branch and even objects.
            case Force.Z(_, vs, _) =>
              vs.collect({
                case x: BoundVar.Z =>
                  x.value
              }).toSet
              // TODO: Handle resolves. They need to be a little different though since for Resolve a single halt will not halt the entire thing.
              /*
            case Resolve.Z(vs, _) =>
              vs.collect({
                case x: BoundVar.Z =>
                  x.value
              }).toSet
              */
            case IfLenientMethod.Z(v, l, r) => {
              v.byIfLenientCases(
                  left = states(EntryNode(l)),
                  right = states(EntryNode(r)),
                  both = Set())
            }
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

      /*Logger.fine(s"""$node: killAll = $killAll gen = $gen
    ins = ${inputs(node).size} ${inputs(node).map(_.node)}
    inState = ${inState.size} ${inState.take(10)}
    state = ${state.size} ${state.take(10)}""")
    */

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = a subsetOf b
  }
}

