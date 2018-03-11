//
// AlreadyForcedAnalysis.scala -- Scala object and class AlreadyForcedAnalysis
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

import orc.ast.orctimizer.named._
import orc.compile.{ AnalysisRunner, AnalysisCache }
import orc.compile.flowanalysis.{ Analyzer, AnalyzerEdgeCache, GraphDataProvider }
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import FlowGraph.{ Node, Edge }
import orc.util.DotUtils.DotAttributes
import orc.compile.orctimizer.FlowGraph.EntryExitEdge

class AlreadyForcedAnalysis(
  val results: collection.Map[Node, Set[Node]],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._

  val edges = graph.edges.filter({ case e: HappensBeforeEdge => true; case _ => false })
  val nodes = edges.flatMap(e => Set(e.from, e.to))
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  /** Return a set containing all the futures which could have been required 
    * to reach the exit node of the provided expression.
    */
  def apply(e: Expression.Z): Set[Node] = {
    results.getOrElse(ExitNode(e), Set())
  }

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    results.get(n) match {
      case Some(s) =>
        Map("label" -> s"${n.label}\n{ ${s.mkString(", ")} }")
      case None => Map()
    }
  }
}

/** Compute a set of futures for each FlowNode such that any future that MUST 
  * have been force on ANY possible path to that node is in the set.
  */
object AlreadyForcedAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), AlreadyForcedAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): AlreadyForcedAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val local = cache.get(LocalNodeAnalysis)(params)
    val a = new AlreadyForcedAnalyzer(cg, local)
    val res = a()

    val r = new AlreadyForcedAnalysis(res.filter({ case (k, v) => v.nonEmpty }), cg)

    r
  }

  class AlreadyForcedAnalyzer(graph: CallGraph, local: LocalNodeAnalysis) extends Analyzer with AnalyzerEdgeCache {
    import CallGraphValues._     
    import graph._
    import graph.NodeInformation._
    
    type NodeT = Node
    type EdgeT = Edge
    type StateT = Set[Node]
    
    val initialState: StateT = Set()
  
    val worstState: StateT = graph.nodes.collect({ 
      case ExitNode(Future.Z(e)) => 
        ExitNode(e)
    }).toSet[Node]

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
        case _: CombinatorInternalOrderEdge => false
        case _ => true
      }
    }
    
    def inputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdgesOf[HappensBeforeEdge].filter(edgePredicate).toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[FutureValueSourceEdge].toSeq.map(e => ConnectedNode(e, e.from))      
    }

    def outputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdgesOf[HappensBeforeEdge].filter(edgePredicate).toSeq.map(e => ConnectedNode(e, e.to)) ++
        node.outEdgesOf[FutureValueSourceEdge].toSeq.map(e => ConnectedNode(e, e.to))
    }

    def transfer(node: Node, old: StateT, states: States): (StateT, Seq[Node]) = {
      lazy val inState = states.inStateReduced[Edge](_ union _)

      val killAll = inputs(node).isEmpty

      lazy val gen: StateT = node match {
        case node @ EntryNode(ast) =>
          import orc.ast.orctimizer.named._
          ast match {
            case Force.Z(_, _, _) | Resolve.Z(_, _) =>
              val vs = ast match {
                case Force.Z(_, vs, _) => vs
                case Resolve.Z(vs, _) => vs
              }
              vs.flatMap(v => valuesOf(v).view.collect({ case FutureValue(_, n) => n })).toSet
            case _ =>
              Set()
          }
        case node @ ExitNode(ast) =>
          import orc.ast.orctimizer.named._
          ast match {
            case IfLenientMethod.Z(v, l, r) => {
              v.byIfLenientCases(
                  left = states(ExitNode(l)),
                  right = states(ExitNode(r)),
                  both = Set())
            }
            case _ =>
              Set()
          }
        case _ =>
          Set()
      }

      val state: StateT = if (killAll) {
        gen
      } else {
        inState ++ gen
      }

      /*Logger.fine(s"""$node: killAll = $killAll gen = $gen
      ins = ${inputs(node).size} ${inputs(node).map(_.node)}
      inState = ${inState.size} ${inState.take(42)}
      state = ${state.size} ${state.take(42)}""")
       */

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = b subsetOf a
  }
}

