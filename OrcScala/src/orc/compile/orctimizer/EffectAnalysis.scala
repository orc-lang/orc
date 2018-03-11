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

import orc.ast.orctimizer.named.{ BoundVar, Branch, Call, Constant, DeclareMethods, DeclareType, Expression, Force, Future, GetField, GetMethod, HasType, IfLenientMethod, Method, New, Otherwise, Parallel, Resolve, Service, Stop, Trim }
import orc.compile.{ AnalysisCache, AnalysisRunner, Logger }
import orc.compile.flowanalysis.{ Analyzer, AnalyzerEdgeCache, DebuggableGraphDataProvider, GraphDataProvider }
import orc.util.DotUtils.DotAttributes

import FlowGraph.{ Edge, Node }


class EffectAnalysis(
  val results: collection.Map[Expression.Z, EffectAnalysis.EffectInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import EffectAnalysis._
  import FlowGraph._

  def edges = graph.edges
  def nodes = graph.nodes
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  def effectsInfoOf(e: Expression.Z): EffectInfo = {
    results.get(e) match {
      case Some(r) =>
        r
      case None =>
        Logger.finest(s"The node $e does not appear in the analysis results. Using top.")
        EffectAnalysis.worstState
    }
  }
  
  def effects(e: Expression.Z): Boolean = {
    effectsInfoOf(e).effects
  }

  def effected(e: Expression.Z): Boolean = {
    effectsInfoOf(e).effected
  }

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (n match {
      case ExitNode(n) =>
        results.get(n)
      case _ =>
        None
    }) match {
      case Some(EffectInfo(s, ed)) =>
        Map("label" -> s"${n.label}\neffect ${if (s) "s" else ""}/${if (ed) "ed" else ""}")
      case None => Map()
    }
  }
}

object EffectAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), EffectAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): EffectAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val local = cache.get(LocalNodeAnalysis)(params)
    val a = new DelayAnalyzer(cg, local)
    val res = a()

    val delay = new EffectAnalysis(res collect { case (ExitNode(e), r) => (e, r) }, cg)
    
    def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
    //println("=============== delay results ---")
    //println(delay.results.par.map(p => s"${shortString(p._1.value)}\t----=========--> ${p._2}").seq.mkString("\n"))
    
    delay    
  }
  
  val worstState: EffectInfo = EffectInfo(true, true)

  case class EffectInfo(effects: Boolean, effected: Boolean) {
    def combine(o: EffectInfo): EffectInfo = {
      EffectInfo(effects || o.effects, effected || o.effected)
    }
    def lessThan(o: EffectInfo): Boolean = {
      effects >= o.effects && effected >= o.effected 
    }
  }

  class DelayAnalyzer(graph: CallGraph, local: LocalNodeAnalysis) extends Analyzer with AnalyzerEdgeCache {
    import graph._
    import graph.NodeInformation._
    
    type NodeT = Node
    type EdgeT = Edge
    type StateT = EffectInfo

    def initialNodes: collection.Seq[Node] = {
      nodesBy({
        case n: ExitNode if !inputs(n).exists(_.node.isInstanceOf[ExitNode]) =>
          n
      }).toSeq ++
      Seq(EverywhereNode)
    }

    val initialState: EffectInfo = EffectInfo(false, false)

    def inputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node match {
        case n: ExitNode =>
          node.inEdgesOf[HappensBeforeEdge].toSeq.filter(_.to.isInstanceOf[ExitNode]).map(e => ConnectedNode(e, e.from)) ++
            node.inEdgesOf[ValueEdge].toSeq.filter(n => n.to.ast.isInstanceOf[New] || n.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.from)) ++        
            node.inEdgesOf[AfterEdge].toSeq.filter(n => n.to.ast.isInstanceOf[Branch]).map(e => ConnectedNode(e, e.from))
        case _ =>
          Seq()
      }
    }

    def outputsCompute(node: Node): collection.Seq[ConnectedNode] = {
      node match {
        case n: ExitNode =>
          node.outEdgesOf[HappensBeforeEdge].toSeq.filter(_.to.isInstanceOf[ExitNode]).map(e => ConnectedNode(e, e.to)) ++        
            node.outEdgesOf[ValueEdge].toSeq.filter(n => n.to.ast.isInstanceOf[New] ||n.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.to)) ++        
            node.outEdgesOf[AfterEdge].toSeq.filter(n => n.to.ast.isInstanceOf[Branch]).map(e => ConnectedNode(e, e.to))
        case _ =>
          Seq()
      }
    }

    def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
      (a, b) match {
        case (Some(a), Some(b)) => Some(f(a, b))
        case (None, Some(b)) => Some(b)
        case (Some(a), None) => Some(a)
        case (None, None) => None
      }

    def transfer(node: Node, old: EffectInfo, states: States): (EffectInfo, Seq[Node]) = {
      lazy val inState = states.inStateReduced[Edge](_ combine _)
      
      val state: StateT = node match {
        case node @ ExitNode(ast) =>
          import orc.ast.orctimizer.named._
          ast match {
            case Future.Z(_) =>
              inState
            case Call.Z(target, args, _) => {
              val (extPubs, intPubs, otherPubs) = target.byCallTargetCases( 
                externals = { vs =>
                  EffectInfo(local.effects(node), local.effected(node))
                }, internals = { vs => 
                  inState 
                }, others = { vs => 
                  worstState 
                }) 
              applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ combine _))(_ combine _).getOrElse(worstState) 
            }
            case IfLenientMethod.Z(v, l, r) => {
              v.byIfLenientCases(
                  left = states(ExitNode(l)),
                  right = states(ExitNode(r)),
                  both = inState)
            }
            case DeclareMethods.Z(callables, _) if callables.exists(_.isInstanceOf[Service.Z]) =>
              // TODO: Can we do better than this? This is terrible. The real thing we need to encode here is that not killing this will have an effect.
              worstState
            case Trim.Z(_) =>
              inState
            case New.Z(_, _, _, _) =>
              inState
            case GetField.Z(_, _) =>
              initialState
            case GetMethod.Z(_) =>
              initialState
            case Otherwise.Z(l, r) =>
              // TODO: Could use publication info to improve this. But doing so would force an ordering on the analyses. Make sure pubs doesn't need this.
              inState
            case Stop.Z() =>
              initialState
            case Force.Z(_, _, _) =>
              inState
            case Resolve.Z(_, _) =>
              inState
            case Branch.Z(_, _, _) =>
              inState
            case _: BoundVar.Z | Parallel.Z(_, _) | Constant.Z(_) |
              DeclareType.Z(_, _, _) | HasType.Z(_, _) | DeclareMethods.Z(_, _) =>
              inState
          }
        case EverywhereNode =>
          worstState
      }

      //Logger.fine(s"$node: state $state")

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = a lessThan b
  }
}

