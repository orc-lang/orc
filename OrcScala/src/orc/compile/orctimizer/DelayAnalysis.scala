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
import orc.ast.orctimizer.named.Method
import orc.compile.AnalysisCache
import scala.reflect.ClassTag
import orc.compile.flowanalysis.LatticeValue
import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.Analyzer
import FlowGraph.{ Node, Edge }
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import orc.util.DotUtils.DotAttributes
import orc.compile.Logger
import orc.ast.orctimizer.named.Future
import orc.ast.orctimizer.named.IfLenientMethod
import orc.values.sites.{Site => ExtSite}
import orc.util.{ TTrue, TFalse, TUnknown }

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
  val results: Map[Expression.Z, DelayAnalysis.DelayInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  import FlowGraph._
  import DelayAnalysis._

  def edges = graph.edges
  def nodes = graph.nodes
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  def delayOf(e: Expression.Z): DelayInfo = {
    results.get(e) match {
      case Some(r) =>
        r
      case None =>
        Logger.finest(s"The node $e does not appear in the analysis results. Using top.")
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

object DelayAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), DelayAnalysis] {
  import FlowGraph._

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): DelayAnalysis = {
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

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[FutureValueSourceEdge].toSeq.map(e => ConnectedNode(e, e.from)) ++
        node.inEdgesOf[ValueEdge].toSeq.filter(_.to.ast.isInstanceOf[Future]).map(e => ConnectedNode(e, e.from))
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdgesOf[HappensBeforeEdge].toSeq.map(e => ConnectedNode(e, e.to)) ++
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
      lazy val inStateAllOf = states.inStateReduced[TransitionEdge](_ combineAllOf _)
      lazy val inStateOneOf = states.inStateReduced[TransitionEdge](_ combineOneOf _)
      lazy val inStateEntryExit = states.inStateReduced[EntryExitEdge](_ combineAllOf _)

      // FIXME: This produces overly aggressive results for self referential values. It states they are instantly available which is not strictly true since the futures in the cycle are the only reason the cycle can complete.

      val state: StateT = node match {
        case node @ EntryNode(ast) =>
          import orc.ast.orctimizer.named._
          import CallGraphValues._
          ast match {
            case Resolve.Z(_, _) =>
              val inState = states.inStateProcessed[FutureValueSourceEdge, Delay](
                  ComputationDelay(),
                  v => v.maxFirstPubDelay min v.maxHaltDelay,
                  _ max _)
              // We know the future binding must have started before the force starts.
              // TODO: We could track other forces and the like to bound this tighter based on the fact we know it was already forced or something that depends on it was already forced.
              DelayInfo(inState, inState)
            case ast@Force.Z(_, _, _) =>
              val inState = states.inStateProcessed[FutureValueSourceEdge, DelayInfo](
                  initialState,
                  v => DelayInfo(v.maxFirstPubDelay, v.maxFirstPubDelay min v.maxHaltDelay),
                  _ combineOneOf _)
              // We know the future binding must have started before the force starts.
              // TODO: We could track other forces and the like to bound this tighter based on the fact we know it was already forced or something that depends on it was already forced.
              //Logger.info(s"Force-Entry: ${node}    $inState\n${inputs(node).map(_.node.toString.take(100))}\n${outputs(node).map(_.node.toString.take(100))}")
              inState
            case _ =>
              initialState
          }
        case node @ ExitNode(ast) =>
          import orc.ast.orctimizer.named._
          import CallGraphValues._
          ast match {
            case Future.Z(_) =>
              val inStateValue = states.inStateReduced[ValueEdge](_ combineAllOf _)
              DelayInfo(ComputationDelay(), inStateValue.maxHaltDelay)
            case Call.Z(target, _, _) => {
              val (extPubs, intPubs, otherPubs) = graph.byCallTargetCases(target)(
                externals = { vs =>
                  vs.collect({
                    case site: ExtSite => DelayInfo(Delay(site.timeToPublish), Delay(site.timeToHalt))
                    case _ => worstState
                  }).reduce(_ combineOneOf _)
                }, internals = { vs =>
                  inStateOneOf
                }, others = { vs =>
                  worstState
                })

              //Logger.info(s"Handling call to ${node} ${target.value}: ${(extPubs, intPubs, otherPubs)}")
                
              applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ combineOneOf _))(_ combineOneOf _).getOrElse(worstState)
            }
            case IfLenientMethod.Z(v, l, r) => {
              graph.byIfLenientCases(v)(
                  left = states(ExitNode(l)),
                  right = states(ExitNode(r)),
                  both = inStateOneOf)
            }
            case Trim.Z(_) =>
              DelayInfo(inStateAllOf.maxFirstPubDelay, inStateAllOf.maxFirstPubDelay min inStateAllOf.maxHaltDelay)
            case New.Z(_, _, _, _) =>
              val inStateValue = states.inStateReduced[ValueEdge](_ combineAllOf _)
              DelayInfo(ComputationDelay(), inStateValue.maxHaltDelay)
            case GetField.Z(_, _) =>
              initialState
            case GetMethod.Z(e) =>
              initialState
            case Otherwise.Z(l, r) =>
              val lState = states(ExitNode(l))
              val rState = states(ExitNode(r))
              DelayInfo(lState.maxFirstPubDelay max (lState.maxHaltDelay + rState.maxFirstPubDelay), lState.maxHaltDelay + rState.maxHaltDelay)
            case Stop.Z() =>
              DelayInfo(IndefiniteDelay(), ComputationDelay())
            case ast@Force.Z(_, _, _) =>
              // We know the future binding must have started before the force starts.
              // TODO: We could track other forces and the like to bound this tighter based on the fact we know it was already forced or something that depends on it was already forced.
              //Logger.info(s"Force-Exit: ${node}    $inStateEntryExit, $inStateAllOf")
              DelayInfo(inStateEntryExit.maxFirstPubDelay + inStateAllOf.maxFirstPubDelay,
                inStateEntryExit.maxHaltDelay + inStateAllOf.maxHaltDelay)
            case Resolve.Z(_, _) =>
              // We know the future binding must have started before the force starts.
              DelayInfo(inStateEntryExit.maxFirstPubDelay + inStateAllOf.maxFirstPubDelay,
                inStateEntryExit.maxHaltDelay + inStateAllOf.maxHaltDelay)
            case Branch.Z(_, _, _) =>
              // This is for the branch after edges from L-exit to exit
              val inStateAfter = states.inStateReduced[CombinatorInternalOrderEdge](_ combineAllOf _)
              DelayInfo(inStateAllOf.maxFirstPubDelay + inStateAfter.maxFirstPubDelay, 
                  inStateAllOf.maxHaltDelay + inStateAfter.maxHaltDelay)
            case _: BoundVar.Z | Parallel.Z(_, _) | Constant.Z(_) |
              DeclareMethods.Z(_, _) | DeclareType.Z(_, _, _) | HasType.Z(_, _) =>
              inStateAllOf
          }
        case _: ValueNode =>
          // All values are available immediately and never execute
          initialState
        case EverywhereNode =>
          worstState
      }

      //Logger.fine(s"$node: state $state")

      (state, Nil)
    }

    def moreCompleteOrEqual(a: StateT, b: StateT): Boolean = a lessThan b
  }
}

