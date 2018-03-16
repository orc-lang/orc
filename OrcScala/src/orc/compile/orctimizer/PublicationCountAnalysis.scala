//
// PublicationCountAnalysis.scala -- Scala object and class PublicationCountAnalysis
// Project OrcScala
//
// Created by amp on May 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.ast.orctimizer.named.{ BoundVar, Branch, Call, Constant, DeclareMethods, DeclareType, Expression, Force, Future, GetField, GetMethod, HasType, IfLenientMethod, Method, New, Otherwise, Parallel, Resolve, Service, Stop, Trim }
import orc.compile.{ AnalysisCache, AnalysisRunner }
import orc.compile.flowanalysis.{ Analyzer, DebuggableGraphDataProvider, GraphDataProvider }
import orc.util.DotUtils.DotAttributes
import orc.values.sites.Range

import FlowGraph.{ CombinatorInternalOrderEdge, ConstantNode, Edge, EntryExitEdge, EntryNode, ExitNode, FutureValueSourceEdge, MethodNode, Node, TransitionEdge, ValueNode, VariableNode }

class PublicationCountAnalysis(
  results: collection.Map[Node, Range],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  def edges = graph.edges
  def nodes = graph.nodes
  def exit = graph.exit
  def entry = graph.entry

  def subgraphs = Set()

  val expressions = results collect {
    case (ExitNode(l), p) => (l, p)
  }
  val values = results collect {
    case (n@ (_: ValueNode), p) => (n, p)
  }

  def publicationsOf(e: Expression.Z) =
    expressions.get(e).getOrElse(PublicationCountAnalysis.defaultResult)

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    results.get(n) match {
      case Some(r) =>
        Map("label" -> s"${n.label}\n${r}")
      case None => Map()
    }
  }
}

/** An analysis to track token multiplicity through expressions.
  *
  * At each exit node the analysis provides a range representing the
  * number of tokens that could exit if one token entered that expression.
  * The analysis takes into account the possible resolutions of futures by
  * tracking value count information along value edges.
  *
  */
object PublicationCountAnalysis extends AnalysisRunner[(Expression.Z, Option[Method.Z]), PublicationCountAnalysis] {

  /*
   * The analysis is starts with the least specific bounds and reduces them until they cannot be reduced any more.
   * The initial bounds are 0-ω and are reduced toward singleton ranges. There is no empty range.
   */

  val defaultResult = Range(0, None)

  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): PublicationCountAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val a = new PublicationCountAnalyzer(cg)
    val res = a()

    new PublicationCountAnalysis(res, cg)
  }

  def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
    (a, b) match {
      case (Some(a), Some(b)) => Some(f(a, b))
      case (None, Some(b)) => Some(b)
      case (Some(a), None) => Some(a)
      case (None, None) => None
    }

  class PublicationCountAnalyzer(graph: CallGraph) extends Analyzer {
    import FlowGraph._
    import graph._
    import graph.NodeInformation._

    type NodeT = Node
    type EdgeT = Edge
    type StateT = Range

    def initialNodes: collection.Seq[Node] = {
      (graph.nodesBy {
        case n @ (ConstantNode(_, _) | VariableNode(_, _)) => n
        case n @ ExitNode(Stop.Z()) => n
      }).toSeq :+ graph.entry
    }
    val initialState = Range(0, None)

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdges.map(e => ConnectedNode(e, e.from)).toSeq
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdges.map(e => ConnectedNode(e, e.to)).toSeq
    }

    val onePublication = Range(1, 1)
    
    val maxPublications = 2

    def boundRange(r: Range) = {
      val rUpperBounded = {
        if (r.maxi.exists(_ > maxPublications))
          Range(r.mini, None)
        else
          r
      }
      if (rUpperBounded.mini > maxPublications)
        Range(maxPublications, rUpperBounded.maxi)
      else
        rUpperBounded
    }

    def transfer(node: Node, old: Range, states: States): (Range, Seq[Node]) = {
      lazy val inStateTokenOneOf = states.inStateReduced[TransitionEdge](_ union _)
      lazy val inStateTokenAllOf = states.inStateReduced[TransitionEdge](_ + _)

      lazy val inStateEntry = states.inStateReduced[EntryExitEdge](combine)
      lazy val inStateFutureValueSource = states.inStateProcessed[FutureValueSourceEdge, Range](Range(0, 1), _.limitTo(1), _ union _)
      
      // The values on Exits are the results and represent the number of tokens that will come out if one token enters the Entry of that AST node.
      // The values on Entries are internal and represent the number of tokens that will enter a nodes main execution if one token tries to enter.
      // This is used for handling force and resolve

      val outState = node match {
        case EntryNode(ast) =>
          ast match {
            case Force.Z(_, _, _) =>
              inStateFutureValueSource
            case _: BoundVar.Z | Branch.Z(_, _, _) | Parallel.Z(_, _) | Future.Z(_) | Constant.Z(_) | Resolve.Z(_, _) |
              Call.Z(_, _, _) | IfLenientMethod.Z(_, _, _) | Trim.Z(_) | DeclareMethods.Z(_, _) | Otherwise.Z(_, _) |
              New.Z(_, _, _, _) | GetField.Z(_, _) | GetMethod.Z(_) | DeclareType.Z(_, _, _) | HasType.Z(_, _) | Stop.Z() =>
              Range(1, 1)
          }
        case node@ExitNode(ast) =>
          ast match {
            case Future.Z(_) =>
              onePublication
            case Call.Z(_, _, _) if AnnotationHack.inAnnotation[SinglePublication](ast) =>
              Range(0, 1)
            case Call.Z(target, args, _) => {
              val (extPubs, intPubs, otherPubs) = target.byCallTargetCases(
                externals = { vs =>
                  vs.collect({
                    // FIXME: Update to use compile time "invoker" API once available. This will avoid problems of too specific results since the Site assumes a specific arity, etc.
                    case site: orc.values.sites.SpecificArity =>
                      if (args.size == site.arity) {
                        site.publications
                      } else {
                        defaultResult
                      }
                    case site: orc.values.sites.SiteMetadata => site.publications
                    case _ => defaultResult
                  }).reduce(_ union _)
                }, internals = { vs =>
                  if (vs.exists({ case _ : Service.Z => true; case _ => false }))
                      inStateTokenOneOf.mayHalt
                  else
                    inStateTokenOneOf
                }, others = { vs =>
                  Range(0, None)
                })
                
              applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ union _))(_ union _).getOrElse(Range(0, None))
            }
            case IfLenientMethod.Z(v, l, r) => {
              v.byIfLenientCases(
                  left = states(ExitNode(l)),
                  right = states(ExitNode(r)),
                  both = inStateTokenOneOf)
            }
            case Trim.Z(_) =>
              inStateTokenAllOf.limitTo(1)
            case New.Z(_, _, _, _) =>
              onePublication
            case GetField.Z(_, _) =>
              onePublication
            case GetMethod.Z(_) =>
              onePublication
            case Otherwise.Z(l, r) =>
              val lState = states(ExitNode(l))
              val rState = states(ExitNode(r))
              if (lState > 0) {
                lState
              } else if (lState only 0) {
                rState
              } else {
                (lState intersect Range(1, None)) union rState
              }
            case Stop.Z() =>
              Range(0, 0)
            case Force.Z(_, _, _) =>
              inStateTokenAllOf * inStateEntry
            case Resolve.Z(_, _) =>
              inStateEntry
            case Branch.Z(_, _, _) =>
              val inStateAfter = states.inStateReduced[CombinatorInternalOrderEdge](combine)
              inStateTokenAllOf * inStateAfter
            case Parallel.Z(_, _) | DeclareMethods.Z(_, _) | DeclareType.Z(_, _, _) | HasType.Z(_, _) =>
              inStateTokenAllOf
            case _: BoundVar.Z | Constant.Z(_) =>
              onePublication
          }
        case VariableNode(x, ast) =>
          ast match {
            case Force.Z(_, _, _) =>
              onePublication
            case New.Z(_, _, _, _) =>
              onePublication
            case DeclareMethods.Z(_, _) | Method.Z(_, _, _, _, _, _) =>
              onePublication
            case Branch.Z(_, _, _) =>
              onePublication
          }
        case ConstantNode(_, _) | MethodNode(_, _) =>
          onePublication
      }
      //Logger.fine(s"Processed $node:  old=$old    out=$outState")
      
      (boundRange(outState), Nil)
    }

    def combine(a: Range, b: Range) = {
      a intersect b
    }

    def moreCompleteOrEqual(a: Range, b: Range): Boolean = {
      a subsetOf b
    }
  }

}
