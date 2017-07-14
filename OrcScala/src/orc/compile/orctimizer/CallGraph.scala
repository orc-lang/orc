//
// CallGraph.scala -- Scala object and class CallGraph
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

/** Compute and store a call graph for the program stored in flowgraph.
  *
  */
class CallGraph(rootgraph: FlowGraph) extends DebuggableGraphDataProvider[Node, Edge] {
  import CallGraph._

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
      case EntryNode(v : Call.Z) => v
    })
  }

  val (additionalEdges, results) = {
    val vrs = new CallGraphAnalyzer(graph)
    val r = vrs()
    (vrs.additionalEdges.toSet ++ vrs.additionalEdgesNonValueFlow, r)
  }

  val edges: collection.Set[Edge] = {
    additionalEdges ++ graph.edges
  }
  lazy val nodes: collection.Set[Node] = {
    edges.flatMap(e => Seq(e.from, e.to))
  }

  def valuesOf[C <: FlowValue : ClassTag](c: Node): BoundedSet[C] = {
    val CType = implicitly[ClassTag[C]]
    assert(CType != implicitly[ClassTag[Nothing]])
    results.get(c) match {
      case Some(r) =>
        r.collect({ case CType(t) => t})
      case None =>
        Logger.finest(s"The node $c does not appear in the analysis results. Using top.")
        MaximumBoundedSet()
    }
  }

  def valuesOf(e: BoundVar): BoundedSet[FlowValue] = {
    results.keys.find(n => n.ast == e && n.isInstanceOf[VariableNode]) match {
      case Some(n) =>
        valuesOf[FlowValue](n)
      case None =>
        Logger.finest(s"The expression $e does not appear in the analysis results. Using top.")
        MaximumBoundedSet()
    }
  }

  def valuesOf(e: Expression.Z): BoundedSet[FlowValue] = {
    e match {
      case a: Argument.Z =>
        valuesOf[FlowValue](ValueNode(a))
      case e =>
        valuesOf[FlowValue](ExitNode(e))
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

object CallGraph extends AnalysisRunner[(Expression.Z, Option[Callable.Z]), CallGraph] {
  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Callable.Z])): CallGraph = {
    val fg = cache.get(FlowGraph)(params)
    
    /*
    println(BoundedSetInstance.ConcreteBoundedSet)
    println(BoundedSetInstance.MaximumBoundedSet)
    println(ObjectHandlingInstance.ObjectRef)
    println(ObjectHandlingInstance.ObjectValue)
    */
    
    new CallGraph(fg)
  }

  @inline
  def ifDebugNode(n: Node)(f: => Unit): Unit = {
//    val s = n.toString()
//    if(s.contains("new SimpleMapWriter { private") || s.contains("new `syncls2052 { `self2053")) {
//      f
//    }
  }

  object BoundedSetInstance extends BoundedSetModule {
    mod =>
    type TU = Value
    type TL = Nothing
    val sizeLimit = 8

    class ConcreteBoundedSet[T >: TL <: TU](s: Set[T]) extends super.ConcreteBoundedSet[T](s) {
      override def union(o: BoundedSet[T]): BoundedSet[T] = o match {
        case ConcreteBoundedSet(s1) =>
          val ss = (s ++ s1)
          
          // For things that we could contain that should be merged using their own rules we have blocks here.
          
          // Due to non-reified types I have casts here, but I sware it's safe.
          // T must be a superclass of Future if s contains any Futures. So it's safe
          // to cast Future to T iff there were Futures in the input.
          val futures = ss.collect({ case f: FutureValue => f })
          def newFuture = FutureValue(futures.map(_.contents).reduce(_ union _), 
              futures.map(_.futureValueSources).reduce(_ ++ _))
          val combinedFuture = if (futures.isEmpty) Set[Future]() else Set(newFuture)
          // Same for FieldFutureValue.
          val ffutures = ss.collect({ case f: FieldFutureValue => f })
          def newFFuture = FieldFutureValue(ffutures.map(_.contents).reduce(_ union _), 
              ffutures.map(_.futureValueSources).reduce(_ ++ _))
          val combinedFFuture = if (ffutures.isEmpty) Set[FieldFutureValue]() else Set(newFFuture)
          // Same for ObjectValue.
          val objs = ss.collect({ case o: ObjectValue => o })
          val groupedObjs = objs.groupBy(_.root).values
          def combinedObjs = groupedObjs.map(_.reduce(_ ++ _)).toSet
          
          mod((ss -- 
              futures.asInstanceOf[Set[T]] ++ combinedFuture.asInstanceOf[Set[T]] -- 
              ffutures.asInstanceOf[Set[T]]) ++ combinedFFuture.asInstanceOf[Set[T]] --
              objs.asInstanceOf[Set[T]] ++ combinedObjs.asInstanceOf[Set[T]])
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

    // For some reason this cannot be an object. It triggers an uninitialized field error.
    val ConcreteBoundedSet = new ConcreteBoundedSetCompanion {
      override def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    }
  }

  type BoundedSet[T >: Nothing <: Value] = BoundedSetInstance.BoundedSet[T]
  val BoundedSet: BoundedSetInstance.type = BoundedSetInstance
  type ConcreteBoundedSet[T >: Nothing <: Value] = BoundedSetInstance.ConcreteBoundedSet[T]
  val ConcreteBoundedSet = BoundedSetInstance.ConcreteBoundedSet
  type MaximumBoundedSet[T >: Nothing <: Value] = BoundedSetInstance.MaximumBoundedSet[T]
  val MaximumBoundedSet = BoundedSetInstance.MaximumBoundedSet

  sealed abstract class Value extends PrecomputeHashcode with Product {
    def subsetOf(o: Value): Boolean
  }

  sealed trait HasFutureValueSources extends Value {
    def futureValueSources: Set[Node]
  }

  // Values that are allowed as flow values (aka nothing with an ObjectRef)
  sealed trait FlowValue extends Value

  // Values that may be contained in a future
  sealed trait FutureContent extends FlowValue

  // Values that may be stored in fields
  sealed trait FieldContent extends Value

  // Values that may be stored in field futures (aka not futures)
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

  case class FutureValue(contents: BoundedSet[FutureContent], futureValueSources: Set[Node]) extends FlowValue with SetValue with HasFutureValueSources {
    def subsetOf(o: Value): Boolean = o match {
      case FutureValue(otherContents, otherSources) =>
        (contents subsetOf otherContents) &&
          (futureValueSources subsetOf otherSources)
      case _ => false
    }
  }

  case class FieldFutureValue(contents: BoundedSet[FieldFutureContent], futureValueSources: Set[Node]) extends FieldContent with SetValue with HasFutureValueSources {
    def subsetOf(o: Value): Boolean = o match {
      case FieldFutureValue(otherContents, otherSources) =>
        (contents subsetOf otherContents) &&
          (futureValueSources subsetOf otherSources)
      case _ => false
    }
  }

  object ObjectHandlingInstance extends ObjectHandling {
    type NodeT = Node
    type StoredValueT = BoundedSet[FieldContent]
    type ResultValueT = BoundedSet[FlowValue]

    val ObjectValueReified = implicitly[ClassTag[ObjectValue]]
    val ObjectRefReified = implicitly[ClassTag[ObjectRef]]

    case class ObjectRef(root: NodeT) extends FieldFutureContent with ObjectRefBase {
      def subsetOf(o: Value): Boolean = o match {
        case ObjectRefValue(`root`) => true
        case _ => false
      }
    }
    object ObjectRef extends ObjectRefCompanion
    val ObjectRefCompanion = ObjectRef

    case class ObjectValue(root: Node, structures: Map[Node, ObjectStructure]) extends
      FlowValue with FutureContent with SetValue with super.ObjectValueBase {

      private def structureToString(t: (Node, ObjectStructure), prefix: String): String = {
        val (n, struct) = t
        def pf(t: (Field, StoredValueT)) = {
          val (f, v) = t
          s"$prefix |   $f -> $v"
        }
        s"$prefix$n ==>${if (struct.nonEmpty) "\n" else ""}${struct.map(pf).mkString("\n")}"
      }

      override def toString() = {
        s"ObjectValue($root,\n${structures.map(structureToString(_, "      ")).mkString("\n  ")})"
        //s"ObjectValue($root, ...)"
      }

      def derefStoredValue(s: BoundedSet[FieldContent]): BoundedSet[FlowValue] = s flatMap[FlowValue] {
        case v: FlowValue =>
          BoundedSet(v)
        case FieldFutureValue(v, s) =>
          BoundedSet(FutureValue(derefFieldFutureContent(v.map(x=>x)), s))
        case ObjectRef(i) =>
          BoundedSet(lookupObject(i))
      }
      def derefFieldFutureContent(s: BoundedSet[FieldFutureContent]): BoundedSet[FutureContent] = s map[FutureContent] {
        case ObjectRef(i) =>
          lookupObject(i)
        case v: FutureContent =>
          v
      }

      def copy(root: FlowGraph.Node, structs: Map[Node, ObjectStructure]): ObjectValue = {
        ObjectValue(root, structs)
      }

      def subsetOf(o: Value): Boolean = o match {
        case o: ObjectValue => super[ObjectValueBase].subsetOf(o)
        case _ => false
      }
    }

    object ObjectValue extends ObjectValueCompanion
  }
  type ObjectInfo = ObjectHandlingInstance.ObjectInfo
  type ObjectValue = ObjectHandlingInstance.ObjectValue
  val ObjectValue = ObjectHandlingInstance.ObjectValue
  type ObjectRefValue = ObjectHandlingInstance.ObjectRef
  val ObjectRefValue = ObjectHandlingInstance.ObjectRef

  type State = BoundedSet[FlowValue]

  type CallLocation = Call.Z

  case class AnalysisLocation[T <: NamedAST](stack: List[CallLocation], node: Node) {
    def limit(n: Int) = AnalysisLocation(stack.take(n), node)
  }

  // TODO: Implement context sensative analysis.

  //val contextLimit = 1

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
        case n @ ExitNode(New.Z(_, _, _, _)) if valueInputs(n).isEmpty => n
        case n @ ExitNode(Stop.Z()) => n
        case n @ ValueNode(Constant(_), _) => n
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
    
    def removeEdge(e: Edge): Boolean = {
      if (additionalEdgesNonValueFlow.contains(e)) {
        additionalEdgesNonValueFlow -= e
        true
      } else {
        false
      }
    }

    // TODO: The filter operations are actually costing a notable amount of time. We may need to build an index or something.
    
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

    def transfer(node: Node, old: State, states: States): (State, Seq[Node]) = {
      // This is a def instead of a val (or lazy val) because it should be recomputed after nodes are added in the nodes computation.
      def inState = states.inStateReduced[ValueFlowEdge](combine _)
      //def inStateUse = states.inStateReduced[UseEdge](combine _)

      ifDebugNode(node) {
        Logger.fine(s"Processing node: $node @ ${node match { case n: WithSpecificAST => n.location; case n => n.ast}}\n$inState\n${inputs(node).map(_.edge)}")
      }

      // Nodes must be computed before the state because the computation adds the additional edges to the list.
      val nodes: Seq[Node] = node match {
        case entry @ EntryNode(n @ Call.Z(target, args, _)) =>
          val exit = ExitNode(n)
          states.get(ValueNode(target)) match {
            case Some(targets) =>
              // TODO: Make sure this properly handles totally unknown calls. MaximumBoundedSet()

              // Select all callables with the correct arity and correct kind.
              val callables = targets.collect({
                case c@CallableValue(callable: Def, _) if callable.formals.size == args.size && n.isInstanceOf[CallDef.Z] =>
                  c
                case c@CallableValue(callable: Site, _) if callable.formals.size == args.size && n.isInstanceOf[CallSite.Z] =>
                  c
                })
              // Build edges for arguments of this call site to all targets
              val argEdges = for {
                cs <- callables.values.toSet[Set[CallableValue]]
                c <- cs
                (formal, actual) <- (c.graph.arguments zip args)
              } yield {
                ValueEdge(ValueNode(actual), formal)
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

      // Add edges we don't actually use in this analysis but do use in later analyses. These don't need to be reported to the analyzer framework.
      node match {
        case entry @ EntryNode(n @ (Force.Z(_, _, _) | Resolve.Z(_, _))) =>
          val exit = ExitNode(n)
          val sources = states.inStateProcessed[UseEdge, Option[Set[Node]]](Some(Set()), _.values map { v =>
            v flatMap[Node, Set[Node]] { 
              case FutureValue(v1, s) => s
              case v1 => Set()
            }
          }, (a, b) => a.flatMap(a1 => b.map(a1 ++ _)))

          sources match {
            case Some(ss) =>
              ss foreach { s =>
                addEdge(FutureValueSourceEdge(s, entry))
                addEdge(FutureValueSourceEdge(s, exit))
              }
            case None =>
              additionalEdgesNonValueFlow filter { e => e.to == entry || e.to == exit } foreach removeEdge
              addEdge(FutureValueSourceEdge(EverywhereNode, entry))
              addEdge(FutureValueSourceEdge(EverywhereNode, exit))
          }
        case _ =>
          ()
      }

      // Compute the new state
      val state: State = node match {
        case ValueNode(Constant(v: ExtSite), _) =>
          inState + ExternalSiteValue(v)
        case ValueNode(Constant(v), _) =>
          inState + DataValue(v)
        case CallableNode(c, g) =>
          inState + CallableValue(c.value, g)
        case VariableNode(v, f: Force.Z) =>
          inState flatMap {
            case FutureValue(s, _) => s.map(x => x: FlowValue)
            case v => BoundedSet(v)
          }
        case VariableNode(v, _) if valueInputs(node).nonEmpty =>
          inState
        case ExitNode(Future.Z(expr)) =>
          val inNode = ExitNode(expr)
          BoundedSet(FutureValue(inState.map({
            case e: FutureContent => e
            case f: FutureValue => throw new AssertionError(s"Futures should never be inside futures\n$expr")
          }), Set(inNode)))
        case ExitNode(FieldAccess.Z(_, f)) =>
          //Logger.fine(s"Processing FieldAccess: $node ($inState)")
          inState flatMap {
            case o: ObjectValue => o.get(f).getOrElse(BoundedSet())
            case _ => MaximumBoundedSet()
          }
        case node @ ExitNode(New.Z(self, _, bindings, _)) =>
          // FIXME: An incorrect value is ending up in the field .x1 in lenient_anon_object_creation
          val structs = ObjectValue.buildStructures(node) { (field, inNode, refObject) =>
            field match {
              case f@FieldFuture(expr) =>
                val vs = states(inNode).map({
                  case e: ObjectRefValue =>
                    throw new AssertionError(s"This is not expected: $e")
                  case o@ObjectValue(nw, structs) =>
                    refObject(o)
                  case e: FieldFutureContent =>
                    e
                  case f: FutureValue =>
                    throw new AssertionError("Futures should never be inside futures")
                })
                BoundedSet[FieldContent](FieldFutureValue(vs, Set(inNode)))
              case f@FieldArgument(a) =>
                states(inNode).map({
                  case e: FieldFutureValue =>
                    throw new AssertionError(s"This is not expected: $e")
                  case o@ObjectValue(nw, structs) =>
                    refObject(o)
                  case e: FieldContent =>
                    e
                  case f: FutureValue =>
                    FieldFutureValue(f.contents.map({
                      case v: FieldFutureContent => v
                      case o: ObjectValue => refObject(o)
                    }), f.futureValueSources)
                })
            }
          }
       
          // Logger.fine(s"Building SFO for: $nw ;;; $field = $fv ;;; Structs = $structs")
          BoundedSet(ObjectValue(node, structs))
        case ExitNode(Call.Z(target, args, _)) =>
          states.get(ValueNode(target)) match {
            case Some(targets) if targets.exists(v => !v.isInstanceOf[CallableValue]) =>
              MaximumBoundedSet()
            case _ =>
              inState
          }
        case ExitNode(Stop.Z()) =>
          initialState
        case ExitNode(_) if valueInputs(node).nonEmpty =>
          // Pass through publications
          inState
        case EntryNode(Call.Z(target, args, _)) =>
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
