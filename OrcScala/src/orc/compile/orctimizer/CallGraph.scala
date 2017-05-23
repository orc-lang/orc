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

  val entry = graph.entry
  val exit = graph.exit

  val subgraphs = Set()

  val callLocations: Set[CallLocation] = {
    graph.nodesBy({
      case EntryNode(v @ SpecificAST(_: Call, _)) => v.asInstanceOf[SpecificAST[Call]]
    })
  }

  val (additionalEdges, results) = {
    val vrs = new CallGraphAnalyzer(graph)
    val r = vrs()
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
    results.get(c) match {
      case Some(r) =>
        r.collect({ case CType(t) => t})
      case None =>
        Logger.fine(s"The node $c does not appear in the analysis results. Using top.")
        MaximumBoundedSet()
    }
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
      (if (n == graph.exit) Map("color" -> "red", "peripheries" -> "2") else Map()) ++
      (results.get(n) match {
        case Some(r) =>
          Map("label" -> s"${n.label}\n${r.values.map(_.mkString("{",",","}")).getOrElse("Universe")}")
        case None => Map()
      })
  }
}

object CallGraph extends AnalysisRunner[(Expression, Option[SpecificAST[Callable]]), CallGraph] {
  def compute(cache: AnalysisCache)(params: (Expression, Option[SpecificAST[Callable]])): CallGraph = {
    val fg = cache.get(FlowGraph)(params)
    new CallGraph(fg)
  }

  @inline
  def ifDebugNode(n: Node)(f: => Unit): Unit = {
//    val s = n.toString()
//    if(s.contains("new SimpleMapWriter { private") || s.contains("new `syncls2052 { `self2053")) {
//      f
//    }
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
          // Same for FieldFutureValue.
          val futures = ss.collect({ case f: FutureValue => f })
          val combinedFuture = if (futures.isEmpty) Set[Future]() else Set(FutureValue(futures.map(_.contents).reduce(_ union _)))
          val ffutures = ss.collect({ case f: FieldFutureValue => f })
          val combinedFFuture = if (ffutures.isEmpty) Set[FieldFutureValue]() else Set(FieldFutureValue(ffutures.map(_.contents).reduce(_ union _)))
          mod((ss -- futures.asInstanceOf[Set[T]] -- ffutures.asInstanceOf[Set[T]]) ++ 
               combinedFuture.asInstanceOf[Set[T]] ++ combinedFFuture.asInstanceOf[Set[T]])
        case MaximumBoundedSet() =>
          MaximumBoundedSet()
      }
      override def subsetOf(o: BoundedSet[T]): Boolean = o match {
        case ConcreteBoundedSet(so) =>
          s forall { vt =>
            so exists { vo =>
              vt subsetOf vo
            }
          }

        case _ => true
      }
    }

    override object ConcreteBoundedSet extends ConcreteBoundedSetObject {
      override def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    }
  }

  import BoundedSet._

  /*
  implicit class ValueBoundedSet[T <: Value](val a: BoundedSet.BoundedSet[T]) {
    def valueSubsetOf[U <: Value](b: BoundedSet.BoundedSet[U]) = {
      val MaximumBoundedSet = BoundedSet.MaximumBoundedSet[FlowValue]()
      (a, b) match {
        case (ConcreteBoundedSet(sa), ConcreteBoundedSet(sb)) =>
          sa forall { va =>
            sb exists { vb =>
              va subsetOf vb
            }
          }
        case (ConcreteBoundedSet(sa), MaximumBoundedSet) =>
          true
        case (MaximumBoundedSet, ConcreteBoundedSet(sb)) =>
          false
        case (MaximumBoundedSet, MaximumBoundedSet) =>
          true
      }
    }
  }
  */

  sealed abstract class Value extends PrecomputeHashcode with Product {
    def subsetOf(o: Value): Boolean
  }

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

  sealed trait SetValue extends Value

  case class DataValue(v: AnyRef) extends SimpleValue {
    def subsetOf(o: Value): Boolean = o match {
      case DataValue(`v`) => true
      case _ => false
    }
  }

  case class CallableValue(callable: Callable, graph: FlowGraph) extends CallTarget {
    override def toString() = s"CallableValue(${shortString(callable)})"
    def subsetOf(o: Value): Boolean = o match {
      case CallableValue(`callable`, _) => true
      case _ => false
    }
  }

  case class ExternalSiteValue(callable: orc.values.sites.Site) extends CallTarget {
    def subsetOf(o: Value): Boolean = o match {
      case ExternalSiteValue(`callable`) => true
      case _ => false
    }
  }

  case class FutureValue(contents: BoundedSet[FutureContent]) extends FlowValue with SetValue {
    def subsetOf(o: Value): Boolean = o match {
      case FutureValue(otherContents) =>
        contents subsetOf otherContents
      case _ => false
    }
  }

  case class FieldFutureValue(contents: BoundedSet[FieldFutureContent]) extends FieldContent with SetValue {
    def subsetOf(o: Value): Boolean = o match {
      case FieldFutureValue(otherContents) =>
        contents subsetOf otherContents
      case _ => false
    }
  }

  case class ObjectRefValue(instantiation: Node) extends FieldFutureContent {
    override def toString() = s"ObjectRefValue(${shortString(instantiation.ast)})"

    def subsetOf(o: Value): Boolean = o match {
      case ObjectRefValue(`instantiation`) => true
      case _ => false
    }
  }

  case class ObjectValue(root: Node, structures: Map[Node, ObjectValue.ObjectStructure]) extends
      FlowValue with FutureContent with SetValue with ObjectHandling {
    type NodeT = Node
    type StoredValueT = BoundedSet[FieldContent]
    type This = ObjectValue

    private def structureToString(t: (Node, ObjectStructure), prefix: String): String = {
      val (n, struct) = t
      def pf(t: (Field, StoredValueT)) = {
        val (f, v) = t
        s"$prefix |   $f -> $v"
      }
      s"$prefix$n ==>${if (struct.nonEmpty) "\n" else ""}${struct.map(pf).mkString("\n")}"
    }

    override def toString() = s"ObjectValue($root,\n${structures.map(structureToString(_, "      ")).mkString("\n  ")})"

    def apply(f: Field): BoundedSet[FlowValue] = {
      def flatten(s: BoundedSet[FieldContent]): BoundedSet[FlowValue] = s flatMap[FlowValue] {
        case v: FlowValue =>
          BoundedSet(v)
        case FieldFutureValue(v) =>
          flatten(v.map(x=>x))
        case ObjectRefValue(i) =>
          BoundedSet(lookupObject(i))
      }

      get(f) map { flatten } getOrElse { BoundedSet() }
    }

    def copyObject(root: FlowGraph.Node, structs: Map[FlowGraph.Node, Map[Field, BoundedSet[FieldContent]]]): ObjectValue = {
      ObjectValue(root, structs)
    }

    def subsetOf(o: Value): Boolean = o match {
      case o: ObjectValue => super.subsetOf(o)
      case _ => false
    }
  }

  object ObjectValue extends ObjectHandlingCompanion {
    type NodeT = Node
    type StoredValueT = BoundedSet[FieldContent]
    type Instance = ObjectValue
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
    type EdgeT = Edge
    type StateT = State

    def initialNodes: collection.Seq[Node] = {
      (graph.nodesBy {
        case n @ CallableNode(_, _) => n
        //case n @ ExitNode(SpecificAST(Call(_, _, _), _)) => n
        case n @ ExitNode(SpecificAST(New(_, _, _, _), _)) if valueInputs(n).isEmpty => n
        case n @ ExitNode(SpecificAST(Stop(), _)) => n
        case n @ ValueNode(Constant(_)) => n
      }).toSeq
    }
    val initialState: BoundedSet[FlowValue] = ConcreteBoundedSet(Set[FlowValue]())

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

    def valueInputs(node: Node): Seq[Edge] = {
      node.inEdgesOf[ValueFlowEdge].toSeq ++ additionalEdges.filter(_.to == node)
    }

    def valueOutputs(node: Node): Seq[Edge] = {
      node.outEdgesOf[ValueFlowEdge].toSeq ++ additionalEdges.filter(_.from == node)
    }

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      valueInputs(node).map(e => ConnectedNode(e, e.from)) ++ node.inEdgesOf[UseEdge].map(e => ConnectedNode(e, e.from))
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      valueOutputs(node).map(e => ConnectedNode(e, e.to)) ++ node.outEdgesOf[UseEdge].map(e => ConnectedNode(e, e.to))
    }

    var timestamp = 0

    def transfer(node: Node, old: State, states: States): (State, Seq[Node]) = {
      def inState = states.inStateReduced[ValueFlowEdge](combine _)

      timestamp += 1

      /*
      The bug appears to be that an old version of a value is stored in the structures of another Object and then reappears. It's not clear how it reappears however.
      The reappearance appears to be a real problem. But it's also not clear now to keep the version of "structures" up to date. Maybe I need to move to a global object table.
      I didn't want to do that, but I do this it is sound as long as objects are distinguished by instantiation point in all places. It should even work if we can additional sensativies as long as the object table is aware of them.
      */

      ifDebugNode(node) {
        Logger.fine(s"Processing node: $node @ ${node match { case n: WithSpecificAST => n.location; case n => n.ast}}\n$inState\n${inputs(node).map(_.edge)}")
      }

      // Nodes must be computed before the state because the computation adds the additional edges to the list.
      val nodes: Seq[Node] = node match {
        case entry @ EntryNode(n @ SpecificAST(call@Call(target, args, _), path)) =>
          val exit = ExitNode(n)
          states.get(ValueNode(target, path)) match {
            case Some(targets) =>
              // TODO: Make sure this properly handles totally unknown calls. MaximumBoundedSet()

              // Select all callables with the correct arity and correct kind.
              val callables = targets.collect({
                case c@CallableValue(callable: Def, _) if callable.formals.size == args.size && call.isInstanceOf[CallDef] =>
                  c
                case c@CallableValue(callable: Site, _) if callable.formals.size == args.size && call.isInstanceOf[CallSite] =>
                  c
                })
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

              newEdges.map(_.to).toSeq
            case None =>
              Nil
          }
        case _ =>
          Nil
      }

      val state: State = node match {
        case ValueNode(Constant(v: ExtSite)) => inState + ExternalSiteValue(v)
        case ValueNode(Constant(v)) => inState + DataValue(v)
        case CallableNode(c, g) => inState + CallableValue(c, g)
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
        case node @ ExitNode(spAst@SpecificAST(New(self, _, bindings, _), _)) =>
          val structs = ObjectValue.buildStructures(node) { (field, inNode, addObject) =>
            field match {
              case f@FieldFuture(expr) =>
                val vs = states(inNode).collect({
                  case e: ObjectRefValue =>
                    throw new AssertionError(s"This is not expected: $e")
                  case o@ObjectValue(nw, structs) =>
                    addObject(o)
                    ObjectRefValue(nw)
                  case e: FieldFutureContent =>
                    e
                  case f: FutureValue =>
                    throw new AssertionError("Futures should never be inside futures")
                })
                BoundedSet[FieldContent](FieldFutureValue(vs))
              case f@FieldArgument(a) =>
                states(inNode).collect({
                  case e: FieldFutureValue =>
                    throw new AssertionError(s"This is not expected: $e")
                  case o@ObjectValue(nw, structs) =>
                    addObject(o)
                    ObjectRefValue(nw)
                  case e: FieldContent =>
                    e
                })
            }
          }

          // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $structs")
          BoundedSet(ObjectValue(node, structs.toMap))
        case ExitNode(SpecificAST(Call(target, args, _), path)) =>
          states.get(ValueNode(target, path)) match {
            case Some(targets) if targets.exists(v => !v.isInstanceOf[CallableValue]) =>
              MaximumBoundedSet()
            case _ =>
              inState
          }
        case ExitNode(SpecificAST(Stop(), _)) =>
          initialState
        case ExitNode(_) if valueInputs(node).nonEmpty =>
          // Pass through publications
          inState
        case EntryNode(SpecificAST(Call(target, args, _), path)) =>
          // We don't really need this result so this value shouldn't matter. We only process these
          // entries because we need to add nodes for them.
          inState
        case EntryNode(_) =>
          // Ignore other entry nodes because they shouldn't matter
          inState
        case _ =>
          Logger.warning(s"Unknown node given worst case result: $node")
          MaximumBoundedSet()
      }

      ifDebugNode(node) {
        Logger.fine(s"Processed node: $node\n$state\n$nodes")
      }

      (state, nodes)
    }

    def combine(a: State, b: State) = a ++ b

    def moreCompleteOrEqual(a: BoundedSet[FlowValue], b: BoundedSet[FlowValue]): Boolean = {
      // TODO: Needs to handle Flow values that are actually sets correctly
      b subsetOf a
    }
  }
}
