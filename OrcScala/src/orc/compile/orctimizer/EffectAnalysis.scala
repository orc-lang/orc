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

import orc.compile.AnalysisRunner
import orc.ast.orctimizer.named.Expression
import orc.ast.orctimizer.named.Callable
import orc.compile.AnalysisCache
import scala.reflect.ClassTag
import orc.compile.flowanalysis.LatticeValue
import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.Analyzer
import FlowGraph.{Node, Edge}
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import orc.util.DotUtils.DotAttributes
import orc.compile.Logger
import orc.ast.orctimizer.named.{Future, New, Branch}
import orc.values.sites.Effects


class EffectAnalysis(
  val results: Map[Expression.Z, EffectAnalysis.EffectInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._
  import EffectAnalysis._

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

object EffectAnalysis extends AnalysisRunner[(Expression.Z, Option[Callable.Z]), EffectAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Callable.Z])): EffectAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val a = new DelayAnalyzer(cg)
    val res = a()

    new EffectAnalysis(res collect { case (ExitNode(e), r) => (e, r) }, cg)
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

  class DelayAnalyzer(graph: CallGraph) extends Analyzer {
    import graph._
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

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node match {
        case n: ExitNode =>
          node.inEdgesOf[HappensBeforeEdge].toSeq.filter(_.to.isInstanceOf[ExitNode]).map(e => ConnectedNode(e, e.from)) ++
            node.inEdgesOf[ValueEdge].toSeq.filter(_.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.from)) ++        
            node.inEdgesOf[UseEdge].toSeq.filter(n => n.to.ast.isInstanceOf[New] || n.to.ast.isInstanceOf[Branch]).map(e => ConnectedNode(e, e.from))
        case _ =>
          Seq()
      }
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node match {
        case n: ExitNode =>
          node.outEdgesOf[HappensBeforeEdge].toSeq.filter(_.to.isInstanceOf[ExitNode]).map(e => ConnectedNode(e, e.to)) ++        
            node.outEdgesOf[ValueEdge].toSeq.filter(_.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.to)) ++        
            node.outEdgesOf[UseEdge].toSeq.filter(n => n.to.ast.isInstanceOf[New] || n.to.ast.isInstanceOf[Branch]).map(e => ConnectedNode(e, e.to))
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
            case CallSite.Z(target, _, _) => {
              import CallGraph.{ FlowValue, ExternalSiteValue, CallableValue }

              val possibleV = graph.valuesOf[FlowValue](ValueNode(target))
              val extPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[ExternalSiteValue]) =>
                  val pubss = s.toSeq.collect {
                    case ExternalSiteValue(site) =>
                      EffectInfo(site.effects != Effects.None, true)
                  }
                  Some(pubss.reduce(_ combine _))
                case _ =>
                  None
              }
              val intPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[CallableValue]) =>
                  Some(inState)
                case _ =>
                  None
              }
              val otherPubs = possibleV match {
                case CallGraph.ConcreteBoundedSet(s) if s.exists(v => !v.isInstanceOf[ExternalSiteValue] && !v.isInstanceOf[CallableValue]) =>
                  Some(worstState)
                case _: CallGraph.MaximumBoundedSet[_] =>
                  Some(worstState)
                case _ =>
                  None
              }
              applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ combine _))(_ combine _).getOrElse(worstState)
            }
            case CallDef.Z(target, _, _) =>
              inState
            case IfDef.Z(v, l, r) => {
              // This complicated mess is cutting around the graph. Ideally this information could be encoded in the graph, but this is flow sensitive?
              import CallGraph.{ FlowValue, ExternalSiteValue, CallableValue }
              val possibleV = graph.valuesOf[CallGraph.FlowValue](ValueNode(v))
              val isDef = possibleV match {
                case _: CallGraph.MaximumBoundedSet[_] =>
                  None
                case CallGraph.ConcreteBoundedSet(s) =>
                  val (ds, nds) = s.partition {
                    case CallableValue(callable: Def, _) =>
                      true
                    case _ =>
                      false
                  }
                  (ds.nonEmpty, nds.nonEmpty) match {
                    case (true, false) =>
                      Some(true)
                    case (false, true) =>
                      Some(false)
                    case _ =>
                      None
                  }
              }
              val realizableIn = isDef match {
                case Some(true) =>
                  states(ExitNode(l))
                case Some(false) =>
                  states(ExitNode(r))
                case None =>
                  inState
              }
              realizableIn
            }
            case DeclareCallables.Z(callables, _) if callables.exists(_.isInstanceOf[Site.Z]) =>
              worstState
            case Trim.Z(_) =>
              inState
            case New.Z(_, _, bindings, _) =>
              inState
            case FieldAccess.Z(_, f) =>
              initialState
            case Otherwise.Z(l, r) =>
              // TODO: Could use publication info to improve this.
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
              DeclareType.Z(_, _, _) | HasType.Z(_, _) | DeclareCallables.Z(_, _) =>
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

