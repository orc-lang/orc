//
// CallGraph.scala -- Scala class CallGraph
// Project OrcScala
//
// Created by amp on Mar 17, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import scala.collection.mutable

import orc.ast.orctimizer.named._
import orc.compile.orctimizer.FlowGraph._
import scala.annotation.tailrec
import scala.collection.mutable.Queue
import orc.compile.flowanalysis.Analyzer
import orc.compile.flowanalysis.BoundedSetModule
import orc.values.Field
import orc.values.sites.{ Site => ExtSite }
import orc.compile.Logger
import orc.ast.PrecomputeHashcode
import scala.reflect.ClassTag
import orc.compile.flowanalysis.GraphDataProvider
import orc.util.DotUtils.shortString
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import orc.util.DotUtils.DotAttributes
import orc.compile.AnalysisCache
import orc.compile.AnalysisRunner
import orc.compile.AnalysisRunner

/** Compute and store a call graph for the program stored in flowgraph.
  *
  */
class CallGraph(rootgraph: FlowGraph) extends DebuggableGraphDataProvider[Node, Edge] {
  import CallGraph._
  import BoundedSet._

  /*

   The call graph is a relation between call sites and targets. It is defined
   by the following rules.

   targets(Call(targetVar, ...)) = valuesReaching(targetVar)

   valuesReaching(x)                              = Union over in value flow edges from nodes y of valuesReaching(y)
                                    if there is an in value flow edge
   valuesReaching(ExitNode(Call(target, args)))   = Union over d = targets(Call(target, ...)) of valuesReaching(d.exit)

   valuesReaching(Constant/Def(d))                = { d }

   valuesReaching(x)                              = ⊤
                                    OTHERWISE

   ⊤ is a value representing that the value could be anything (including values that do no appear in the program).

   TODO: It would be possible to extend this analysis differentiate between any value at all (⊤) and only values not in the program.
         This would be useful when using the inverse of this relation since any ⊤ site would call every target in the program.
   TODO: This could incorporate Orc types (runtime types). This would enable nice general information and work well with site
         metadata.

   */

  val graph: FlowGraph = rootgraph.combinedGraph

  val subgraphs = Set()

  val callLocations: Set[CallLocation] = {
    graph.nodesBy({
      case EntryNode(v @ SpecificAST(_: Call, _)) => v.asInstanceOf[SpecificAST[Call]]
    })
  }

  val (additionalEdges, results) = {
    val vrs = new CallGraphAnalyzer(graph)
    val r = vrs().filterNot(_._2.isInstanceOf[MaximumBoundedSet[_]])
    (vrs.additionalEdges ++ vrs.additionalEdgesNonValueFlow, r)
  }

  val edges: collection.Set[Edge] = {
    additionalEdges ++ graph.edges
  }
  lazy val nodes: collection.Set[Node] = {
    edges.flatMap(e => Seq(e.from, e.to))
  }

  def callTargets(c: Call): BoundedSet[CallTarget] = {
    results.keys.find(_.ast == c.target) match {
      case Some(n) =>
        valuesOf[CallTarget](n)
      case None =>
        Logger.fine(s"The call $c does not appear in the analysis results. Using top.")
        MaximumBoundedSet()
    }
  }

  def valuesOf[C <: FlowValue : ClassTag](c: Node): BoundedSet[C] = {
    val CType = implicitly[ClassTag[C]]
    results(c).collect({ case CType(t) => t})
  }

  def valuesOf(e: Expression): BoundedSet[FlowValue] = {
    results.keys.find(_.ast == e) match {
      case Some(n) =>
        valuesOf[FlowValue](n)
      case None =>
        Logger.fine(s"The expression $e does not appear in the analysis results. Using top.")
        MaximumBoundedSet()
    }
  }

  override def graphLabel: String = "Call Graph"

  /*
  override def renderedNodes = {
    for {
      n @ (ExitNode(_) | EntryNode(_)) <- nodes
    } yield {
      n
    }
  }
  override def renderedEdges = {
    for {
      e @ TransitionEdge(a: TokenFlowNode, trans, b: TokenFlowNode) <- edges
      //if a.location != b.location
    } yield {
      e
    }
  }
  */

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (if (n == graph.entry) Map("color" -> "green", "peripheries" -> "2") else Map()) ++
      (if (n == graph.exit) Map("color" -> "red", "peripheries" -> "2") else Map())
  }
}

object CallGraph extends AnalysisRunner[(Expression, Option[SpecificAST[Callable]]), CallGraph] {
  def compute(cache: AnalysisCache)(params: (Expression, Option[SpecificAST[Callable]])): CallGraph = {
    val fg = cache.get(FlowGraph)(params)
    new CallGraph(fg)
  }

  val BoundedSet: BoundedSetModule {
    type TU = Value
    type TL = Nothing
  } = new BoundedSetModule {
    mod =>
    type TU = Value
    type TL = Nothing
    val sizeLimit = 8

    override def apply[T >: TL <: TU](s: Set[T]): BoundedSet[T] = {
      if (s.size > sizeLimit) {
        MaximumBoundedSet()
      } else {
        ConcreteBoundedSet(s)
      }
    }

    class ConcreteBoundedSet[T >: TL <: TU](s: Set[T]) extends super.ConcreteBoundedSet[T](s) {
      override def union(o: BoundedSet[T]): BoundedSet[T] = o match {
        case ConcreteBoundedSet(s1) =>
          val ss = (s ++ s1)
          // Due to non-reified types I have casts here, but I sware it's safe.
          // T must be a superclass of Future if s contains any Futures. So it's safe
          // to cast Future to T iff there were Futures in the input.
          val futures = ss.collect({ case f: FutureValue => f })
          val combinedFuture = if (futures.isEmpty) Set[Future]() else Set(FutureValue(futures.map(_.callable).reduce(_ union _)))
          mod((ss -- futures.asInstanceOf[Set[T]]) ++ combinedFuture.asInstanceOf[Set[T]])
        case MaximumBoundedSet() =>
          MaximumBoundedSet()
      }
    }

    override object ConcreteBoundedSet extends ConcreteBoundedSetObject {
      override def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    }
  }

  import BoundedSet._

  sealed abstract class Value extends PrecomputeHashcode with Product

  // Values that are allowed as flow values (aka nothing with an ObjectRef)
  sealed trait FlowValue extends Value

  // Values that may be contained in a future
  sealed trait FutureContent extends FlowValue

   // Values that may be stored in fields
  sealed trait FieldContent extends Value

   // Values that may be stored in fields
  sealed trait FieldFutureContent extends FieldContent

  sealed trait SimpleValue extends FutureContent with FieldFutureContent with FlowValue

  sealed trait CallTarget extends SimpleValue

  case class DataValue(v: AnyRef) extends SimpleValue

  case class CallableValue(callable: Callable, graph: FlowGraph) extends CallTarget {
    override def toString() = s"CallableValue(${shortString(callable)})"
  }

  case class ExternalSiteValue(callable: orc.values.sites.Site) extends CallTarget {
  }

  case class FutureValue(callable: BoundedSet[FutureContent]) extends FlowValue

  case class FieldFutureValue(callable: BoundedSet[FieldFutureContent]) extends FieldContent

  case class ObjectRefValue(instantiation: Node) extends FieldFutureContent {
    override def toString() = s"ObjectRefValue(${shortString(instantiation.ast)})"
  }

  type ObjectStruture = Map[Field, BoundedSet[FieldContent]]

  case class ObjectValue(root: Node, structures: Map[Node, ObjectStruture]) extends FlowValue with FutureContent {
    require(structures contains root, s"Root $root is not available in $structures")

    override def toString() = s"ObjectValue($root, ${structures(root)}, ${structures.keySet})"

    def apply(f: Field): BoundedSet[FlowValue] = {
      val self = structures(root)
      def flatten(s: BoundedSet[FieldContent]): BoundedSet[FlowValue] = s flatMap[FlowValue] {
        case v: FlowValue => BoundedSet(v)
        case FieldFutureValue(v) => flatten(v.map(x=>x))
        case ObjectRefValue(i) =>
          assert(structures contains i, s"Root $i is not available in $this")
          BoundedSet(ObjectValue(i, structures))
      }

      self.get(f) map { flatten } getOrElse { MaximumBoundedSet() }
    }

    private def mergeMaps[K, V](m1: Map[K, V], m2: Map[K, V])(f: (V, V) => V): Map[K, V] = {
      m1 ++ m2.map { case (k, v) => k -> m1.get(k).map(f(v, _)).getOrElse(v) }
    }

    def ++(o: ObjectValue): ObjectValue = {
      require(root == o.root)
      val newStruct = mergeMaps(structures, o.structures) { (s1, s2) =>
        mergeMaps(s1, s2) { _ ++ _ }
      }
      ObjectValue(root, newStruct)
    }
  }

  type State = BoundedSet[FlowValue]

  type CallLocation = SpecificAST[Call]

  case class AnalysisLocation[T <: NamedAST](stack: List[CallLocation], node: Node) {
    def limit(n: Int) = AnalysisLocation(stack.take(n), node)
  }

  // TODO: Implement context sensative analysis.

  val contextLimit = 1

  class CallGraphAnalyzer(graph: GraphDataProvider[Node, Edge]) extends Analyzer {
    import graph._
    import FlowGraph._

    type NodeT = Node
    type StateT = State

    def initialNodes: collection.Set[Node] = {
      graph.nodesBy {
        case n @ CallableNode(_, _) => n
        //case n @ ExitNode(SpecificAST(Call(_, _, _), _)) => n
        case n @ ExitNode(SpecificAST(New(_, _, _, _), _)) if valueInputs(n).isEmpty => n
        case n @ ValueNode(Constant(_)) => n
      }
    }
    def initialState: BoundedSet[FlowValue] = ConcreteBoundedSet(Set[FlowValue]())

    val additionalEdges = mutable.HashSet[ValueFlowEdge]()
    val additionalEdgesNonValueFlow = mutable.HashSet[Edge]()

    def addEdge(e: ValueFlowEdge): Boolean = {
      if (additionalEdges.contains(e) || edges.contains(e)) {
        false
      } else {
        // Logger.fine(s"Adding edge $e")
        additionalEdges += e
        true
      }
    }

    def addEdge(e: Edge): Boolean = {
      if (additionalEdgesNonValueFlow.contains(e) || edges.contains(e)) {
        false
      } else {
        // Logger.fine(s"Adding edge $e")
        additionalEdgesNonValueFlow += e
        true
      }
    }

    def valueInputs(node: Node): Set[ValueFlowEdge] = {
      node.inEdgesOf[ValueFlowEdge] ++ additionalEdges.filter(_.to == node)
    }

    def valueOutputs(node: Node): Set[ValueFlowEdge] = {
      node.outEdgesOf[ValueFlowEdge] ++ additionalEdges.filter(_.from == node)
    }

    def inputs(node: Node): collection.Set[Node] = {
      valueInputs(node).map(_.from) ++ node.inEdgesOf[UseEdge].map(_.from).filter(_.ast.isInstanceOf[Call])
    }

    def outputs(node: Node): collection.Set[Node] = {
      valueOutputs(node).map(_.to) ++ node.outEdgesOf[UseEdge].map(_.to).filter(_.ast.isInstanceOf[Call])
    }

    def transfer(node: Node, old: State, inState: State, states: StateMap): (State, Set[Node]) = {
      def buildSingleFieldObject(nw: SpecificAST[New], field: Field)(fieldValue: mutable.Map[Node, ObjectStruture] => BoundedSet[FieldContent]): State = {
        val nwn = ExitNode(nw)
        val additionalStructures = mutable.Map[Node, ObjectStruture]()
        val fv = fieldValue(additionalStructures)
        val structs = additionalStructures + (nwn -> Map[Field, BoundedSet[FieldContent]](field -> fv))
        // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $structs")
        BoundedSet(ObjectValue(nwn, structs.toMap))
      }

      // Logger.fine(s"Processing node: ${shortString(node)}    $inState ${inputs(node)}")
      val state: State = node match {
        case ValueNode(Constant(v: ExtSite)) => inState + ExternalSiteValue(v)
        case ValueNode(Constant(v)) => inState + DataValue(v)
        case CallableNode(c, g) => inState + CallableValue(c, g)
        case FutureFieldNode(nw, field) =>
          buildSingleFieldObject(nw, field) { additionalStructures =>
            val vs = inState.collect({
              case e: ObjectRefValue =>
                throw new AssertionError(s"This is not expected: $e")
              case ObjectValue(nw, structs) =>
                additionalStructures ++= structs
                ObjectRefValue(nw)
              case e: FieldFutureContent => e
              case f: FutureValue =>
                throw new AssertionError("Futures should never be inside futures")
            })
            BoundedSet[FieldContent](FieldFutureValue(vs))
          }
        case ArgumentFieldNode(nw, field) =>
          buildSingleFieldObject(nw, field) { additionalStructures =>
            inState.collect({
              case e: FieldFutureValue =>
                throw new AssertionError(s"This is not expected: $e")
              case ObjectValue(nw, structs) =>
                additionalStructures ++= structs
                ObjectRefValue(nw)
              case e: FieldContent => e
            })
          }
        case VariableNode(v, f: Force) =>
          // FIXME: This needs to correctly handle the two kinds of force. Including messing with objects and values that may have apply.
          inState flatMap {
            case FutureValue(s) => s.map(x => x: FlowValue)
            case v => BoundedSet(v)
          }
        case VariableNode(v, _) if valueInputs(node).nonEmpty =>
          inState
        case ExitNode(SpecificAST(Future(_), _)) =>
          BoundedSet(FutureValue(inState.collect({
            case e: FutureContent => e
            case f: FutureValue => throw new AssertionError("Futures should never be inside futures")
          })))
        case ExitNode(SpecificAST(FieldAccess(_, f), _)) =>
          //Logger.fine(s"Processing FieldAccess: $node ($inState)")
          inState flatMap { s =>
            s match {
              case o: ObjectValue => o(f)
              case _ => MaximumBoundedSet()
            }
          }
        case node @ ExitNode(SpecificAST(_: New, _)) =>
          inState modify { s =>
            val bss = s.collect({
              case o@ObjectValue(nw1, bs) if node == nw1 => o
              case ObjectValue(nw1, bs) =>
                throw new AssertionError(s"A 'New' node received the incorrent object input during analysis: $nw1 Expected: $node")
              case v =>
                throw new AssertionError(s"A 'New' node received an non-object input during analysis: $v")
            })

            Set(bss.fold(ObjectValue(node, Map(node -> Map())))(_ ++ _))
          }
        case ExitNode(_) if valueInputs(node).nonEmpty =>
          // Passthrough on publications
          inState
        case EntryNode(SpecificAST(Call(target, args, _), path)) =>
          // We don't really need this result so this value shouldn't matter. We only process these
          // entries because we need to add nodes for them.
          MaximumBoundedSet()
        case _ =>
          // Logger.warning(s"Unknown node given worst case result: $node")
          MaximumBoundedSet()
      }

      val nodes: Set[Node] = node match {
        case entry @ EntryNode(n @ SpecificAST(Call(target, args, _), path)) =>
          val exit = ExitNode(n)
          states.get(ValueNode(target, path)) match {
            case Some(targets) =>
              // TODO: Make sure this properly handles totally unknown calls. MaximumBoundedSet()

              // Select all callables with the correct arity.
              val callables = targets.collect({ case c: CallableValue if c.callable.formals.size == args.size => c })
              // Build edges for arguments of this call site to all targets
              val argEdges = for {
                cs <- callables.values.toSet[Set[CallableValue]]
                c <- cs
                (formal, actual) <- (c.graph.arguments zip args)
              } yield {
                ValueEdge(ValueNode(actual, path), formal)
              }
              // Build edges for return value of this call site from all targets
              val retEdges = for {
                cs <- callables.values.toSet[Set[CallableValue]]
                c <- cs
              } yield {
                ValueEdge(c.graph.exit, exit)
              }
              // The filter has a side effects. I feel appropriately bad about myself.
              val newEdges = (argEdges ++ retEdges) filter addEdge

              // Add transitions into and out of the function body based on target results.
              for {
                cs <- callables.values.toSet[Set[CallableValue]]
                c <- cs
              } yield {
                 addEdge(TransitionEdge(entry, "Call-Inf", c.graph.entry))
                 addEdge(TransitionEdge(c.graph.exit, "Return-Inf", exit))
              }

              newEdges.map(_.to)
            case None =>
              Set()
          }
        case _ =>
          Set()
      }

      // Logger.fine(s"Processed node: ${shortString(node)} ;;; $state ;;; $nodes")

      (state, nodes)
    }

    def combine(a: State, b: State) = a ++ b
  }
}
