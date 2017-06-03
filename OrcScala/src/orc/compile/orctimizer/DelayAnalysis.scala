//
// DelayAnalysis.scala -- Scala object and class DelayAnalysis
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
import orc.ast.orctimizer.named.SpecificAST
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
import orc.ast.orctimizer.named.Future

sealed abstract class Delay {
  def <(o: Delay): Boolean
  def >(o: Delay): Boolean = o < this
  def <=(o: Delay): Boolean = this < o || this == o
  def >=(o: Delay): Boolean = o < this || this == o
  def max(o: Delay) =
    if (this < o) o else this
  def min(o: Delay) =
    if (this < o) this else o

  // Summing is the same as mas because of the large granularity of the information. This could actually do summing later.
  def +(o: Delay): Delay = this max o
}
object Delay {
  def apply(o: orc.values.sites.Delay): Delay = o match {
    case orc.values.sites.Delay.NonBlocking =>
      ComputationDelay()
    case orc.values.sites.Delay.Blocking =>
      BlockingDelay()
    case orc.values.sites.Delay.Forever =>
      IndefiniteDelay()
  }
}

/** The delay is not related to any extrnal effect.
  *
  * Completion may still depend on computation.
  */
case class ComputationDelay() extends Delay {
  def <(o: Delay): Boolean = o != ComputationDelay()
}
// TODO: ComputationDelay could hold a cost value which measures the relative cost of different paths.

/** The delay is determined by some external event (such as an effect of another expression).
  */
case class BlockingDelay() extends Delay {
  def <(o: Delay): Boolean = o match {
    case ComputationDelay() => false
    case BlockingDelay() => false
    case _ => true
  }
}
// TODO: BlockingDelay could track futures which are being blocked on before we get here. That would allow us to detect cases where a future will already be forced due to, for example, forcing a future which depends on it.
// TODO: BlockingDelay could have a values: CallGraph.State which represents the set of values which this call could block waiting for effects on.
// To be useful this would need some form of value tracking/numbering and related meta-information on instantiation sites.

/** The delay is indefinite, meaning it will never complete.
  */
case class IndefiniteDelay() extends Delay {
  def <(o: Delay): Boolean = false
}

class DelayAnalysis(
  val results: Map[SpecificAST[Expression], DelayAnalysis.DelayInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._
  import DelayAnalysis._

  def edges = graph.edges
  def nodes = graph.nodes
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  def delayOf(e: SpecificAST[Expression]): DelayInfo = {
    results.get(e) match {
      case Some(r) =>
        r
      case None =>
        Logger.fine(s"The node $e does not appear in the analysis results. Using top.")
        DelayAnalysis.worstState
    }
  }

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (n match {
      case ExitNode(n) =>
        results.get(n)
      case _ =>
        None
    }) match {
      case Some(DelayInfo(p, h)) =>
        Map("label" -> s"${n.label}\n${p} | ${h}")
      case None => Map()
    }
  }
}

object DelayAnalysis extends AnalysisRunner[(Expression, Option[SpecificAST[Callable]]), DelayAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression, Option[SpecificAST[Callable]])): DelayAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val a = new DelayAnalyzer(cg)
    val res = a()

    new DelayAnalysis(res collect { case (ExitNode(e), r) => (e, r) }, cg)
  }
  
  val worstState: DelayInfo = DelayInfo(IndefiniteDelay(), IndefiniteDelay())

  case class DelayInfo(maxFirstPubDelay: Delay, maxHaltDelay: Delay) {
    def combineAllOf(o: DelayInfo): DelayInfo = {
      DelayInfo(maxFirstPubDelay min o.maxFirstPubDelay,
        maxHaltDelay max o.maxHaltDelay)
    }
    def combineOneOf(o: DelayInfo): DelayInfo = {
      DelayInfo(maxFirstPubDelay max o.maxFirstPubDelay,
        maxHaltDelay max o.maxHaltDelay)
    }
    def lessThan(o: DelayInfo): Boolean = {
      maxFirstPubDelay >= o.maxFirstPubDelay &&
        maxHaltDelay >= o.maxHaltDelay
    }
  }

  class DelayAnalyzer(graph: CallGraph) extends Analyzer {
    import graph._
    type NodeT = Node
    type EdgeT = Edge
    type StateT = DelayInfo

    def initialNodes: collection.Seq[Node] = {
      Seq(entry, EverywhereNode)
    }

    val initialState: DelayInfo = DelayInfo(ComputationDelay(), ComputationDelay())

    // TODO: I only actually need the value edges into future nodes.
    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[UseEdge].toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[FutureValueSourceEdge].toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[ValueEdge].toSeq.filter(_.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.from))
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.to)) ++
        node.outEdgesOf[UseEdge].toSeq.map(e => ConnectedNode(e, e.to)) ++
        node.outEdgesOf[FutureValueSourceEdge].toSeq.map(e => ConnectedNode(e, e.to)) ++
        node.outEdgesOf[ValueEdge].toSeq.filter(_.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.to))
    }

    def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
      (a, b) match {
        case (Some(a), Some(b)) => Some(f(a, b))
        case (None, Some(b)) => Some(b)
        case (Some(a), None) => Some(a)
        case (None, None) => None
      }

    def transfer(node: Node, old: DelayInfo, states: States): (DelayInfo, Seq[Node]) = {
      lazy val inStateAllOf = states.inStateReduced[HappensBeforeEdge](_ combineAllOf _)
      lazy val inStateOneOf = states.inStateReduced[HappensBeforeEdge](_ combineOneOf _)
      lazy val inStateValue = states.inStateReduced[ValueEdge](_ combineOneOf _)
      // This is for the branch use edges from L-exit to exit
      lazy val inStateUse = states.inStateReduced[UseEdge](_ combineAllOf _)
      lazy val inStateFutureValueSource = states.inStateReduced[FutureValueSourceEdge](_ combineOneOf _)
      
      val state: StateT = node match {
        case node @ ExitNode(spAst @ SpecificAST(ast, _)) =>
          import orc.ast.orctimizer.named._
          ast match {
            case Future(_) =>
              DelayInfo(ComputationDelay(), inStateValue.maxHaltDelay)
            case CallSite(target, _, _) => {
              import CallGraph.{ FlowValue, ExternalSiteValue, CallableValue }

              val possibleV = graph.valuesOf[FlowValue](ValueNode(target, spAst.subtreePath))
              val extPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[ExternalSiteValue]) =>
                  val pubss = s.toSeq.collect {
                    case ExternalSiteValue(site) =>
                      DelayInfo(Delay(site.timeToPublish), Delay(site.timeToHalt))
                  }
                  Some(pubss.reduce(_ combineOneOf _))
                case _ =>
                  None
              }
              val intPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[CallableValue]) =>
                  Some(inStateOneOf)
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
              applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ combineOneOf _))(_ combineOneOf _).getOrElse(worstState)
            }
            case CallDef(target, _, _) =>
              inStateOneOf
            case IfDef(v, l, r) => {
              // This complicated mess is cutting around the graph. Ideally this information could be encoded in the graph, but this is flow sensitive?
              import CallGraph.{ FlowValue, ExternalSiteValue, CallableValue }
              val possibleV = graph.valuesOf[CallGraph.FlowValue](ValueNode(v, spAst.subtreePath))
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
                  states(ExitNode(SpecificAST(l, spAst.subtreePath)))
                case Some(false) =>
                  states(ExitNode(SpecificAST(r, spAst.subtreePath)))
                case None =>
                  inStateOneOf
              }
              realizableIn
            }
            case Trim(_) =>
              DelayInfo(inStateAllOf.maxFirstPubDelay, inStateAllOf.maxFirstPubDelay)
            case New(_, _, bindings, _) =>
              DelayInfo(inStateAllOf.maxFirstPubDelay, inStateUse.maxHaltDelay)
            case FieldAccess(_, f) =>
              inStateAllOf
            case Otherwise(l, r) =>
              val lState = states(ExitNode(SpecificAST(l, spAst.subtreePath)))
              val rState = states(ExitNode(SpecificAST(r, spAst.subtreePath)))
              DelayInfo(lState.maxFirstPubDelay max (lState.maxHaltDelay + rState.maxFirstPubDelay), lState.maxHaltDelay + rState.maxHaltDelay)
            case Stop() =>
              DelayInfo(IndefiniteDelay(), ComputationDelay())
            case Force(_, _, b, _) =>
              // We know the future binding must have started before the force starts.
              // TODO: We could track other forces and the like to bound this tighter based on the fact we know it was already forced or something that depends on it was already forced.
              DelayInfo(inStateFutureValueSource.maxFirstPubDelay + inStateAllOf.maxFirstPubDelay, 
                  (inStateFutureValueSource.maxFirstPubDelay min inStateFutureValueSource.maxHaltDelay) + inStateAllOf.maxHaltDelay)
            case Branch(_, _, _) =>
              DelayInfo(inStateAllOf.maxFirstPubDelay + inStateUse.maxFirstPubDelay, inStateAllOf.maxHaltDelay + inStateUse.maxHaltDelay)
            case _: BoundVar | Parallel(_, _) | Constant(_) |
              DeclareCallables(_, _) | DeclareType(_, _, _) | HasType(_, _) =>
              inStateAllOf
          }
        case node @ EntryNode(spAst @ SpecificAST(ast, _)) =>
          initialState
        case _: ValueFlowNode =>
          // All values are available immediately and never execute
          initialState 
        case EverywhereNode =>
          worstState
        case _ =>
          Logger.warning(s"Unknown node given worst case result: $node")
          worstState
      }

      //Logger.fine(s"$node: state $state")

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = a lessThan b
  }
}

