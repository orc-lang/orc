//
// FlowGraph.scala -- Scala class FlowGraph
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
import scala.reflect.ClassTag

import orc.ast.PrecomputeHashcode
import orc.ast.orctimizer.named._
import orc.compile.Logger
import orc.values.Field

import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.EdgeBase
import orc.util.DotUtils._
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import orc.compile.AnalysisRunner
import orc.compile.AnalysisCache

/** A control flow graph for an Orc program which represents the flow of tokens through the program.
  *
  */
class FlowGraph(val root: Expression.Z, val location: Option[Callable.Z] = None) extends DebuggableGraphDataProvider[FlowGraph.Node, FlowGraph.Edge] {
  outer =>

  import FlowGraph._

  protected[this] val nodeSet = mutable.HashSet[Node]()
  protected[this] val edgeSet = mutable.HashSet[Edge]()

  protected[this] lazy val computed = {
    compute()
    assert(nodeSet contains entry)
    assert(nodeSet contains exit)
    true
  }

  def nodes: collection.Set[Node] = { computed; nodeSet }
  def edges: collection.Set[Edge] = { computed; edgeSet }

  def subflowgraphs = nodes.collect({ case CallableNode(_, g) => g }).toSet
  def subgraphs = subflowgraphs
  def allflowgraphs: Set[FlowGraph] = nodes.collect({ case CallableNode(_, g) => g.allflowgraphs }).flatten.toSet + this

  def combinedGraph: FlowGraph = new FlowGraph(root, location) {
    override def compute() = {
      for (g <- outer.allflowgraphs) {
        this.nodeSet ++= g.nodes
        this.edgeSet ++= g.edges
      }
    }
  }

  // TODO: addEdges is a surprisingly large time sink. It may be useful to optimize it somehow.
  // If we are not accessing the sets during processing maybe we could actually build a simpler 
  // data structure and then dump it more efficiently into the HashMaps.
  // If this turns into a large problem we could even use a bloom filter (modified to only have 
  // false negatives) to approximate the sets and check the bloom filter before inserting into
  // the actual HastSet.
  protected[this] def addEdges(es: Edge*): Unit = {
    for (e <- es) {
      edgeSet += e
      nodeSet += e.to
      nodeSet += e.from
    }
  }

  val entry = EntryNode(root)
  val exit = ExitNode(root)

  def arguments: Seq[VariableNode] = {
    location.map(l => l.value.formals.map(VariableNode(_, l.parents))).getOrElse(Seq())
  }

  protected[this] def compute(): Unit = {
    def process(e: Expression.Z): Unit = {
      def declareVariable(e: Node, a: BoundVar) = {
        //addEdges(DefEdge(e, ValueNode(a)))
      }

      val entry = EntryNode(e)
      val exit = ExitNode(e)

      // Add nodes that we process even if they don't have edges
      nodeSet ++= Set(entry, exit)

      e match {
        case Stop.Z() =>
          ()
        case FieldAccess.Z(a, f) =>
          addEdges(
              // TODO: Should this value edge be a UseEdge?
            ValueEdge(ValueNode(a), exit),
            TransitionEdge(entry, "FieldAccess", exit))
        case nw @ New.Z(self, selfT, bindings, objT) =>
          declareVariable(entry, self)
          addEdges(
            ValueEdge(exit, VariableNode(self, nw.parents)))
          addEdges(
            TransitionEdge(entry, "New-Obj", exit))
          for ((f, b) <- bindings) {
            b match {
              case FieldFuture.Z(se) =>
                //Logger.fine(s"Processing field $b with $potentialPath")
                //val tmp = FutureFieldNode(nw, f)
                addEdges(
                  TransitionEdge(entry, "New-Spawn", EntryNode(se)),
                  UseEdge(ExitNode(se), exit))
                process(se)
              case FieldArgument.Z(v) =>
                addEdges(
                  UseEdge(ValueNode(v), exit))
            }
          }
        case Branch.Z(f, x, g) =>
          declareVariable(entry, x)
          addEdges(
            TransitionEdge(entry, "Bra-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Bra-PubL", EntryNode(g)),
            TransitionEdge(ExitNode(g), "Bra-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(f), VariableNode(x, exit.location)),
            ValueEdge(ExitNode(g), exit))
          addEdges(
            UseEdge(ExitNode(f), exit))
          process(f)
          process(g)
        case Otherwise.Z(f, g) =>
          addEdges(
            TransitionEdge(entry, "Otw-Entry", EntryNode(f)),
            AfterHaltEdge(entry, "Otw-Halt", EntryNode(g)),
            TransitionEdge(ExitNode(f), "Otw-PubL", exit),
            TransitionEdge(ExitNode(g), "Otw-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit))
          process(f)
          process(g)
        case Parallel.Z(f, g) =>
          addEdges(
            TransitionEdge(entry, "Par-Enter", EntryNode(f)),
            TransitionEdge(entry, "Par-Enter", EntryNode(g)),
            TransitionEdge(ExitNode(f), "Par-PubL", exit),
            TransitionEdge(ExitNode(g), "Par-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit))
          process(f)
          process(g)
        case Future.Z(f) =>
          addEdges(
            TransitionEdge(entry, "Future-Spawn", EntryNode(f)),
            TransitionEdge(entry, "Future-Future", exit))
          addEdges(
            ValueEdge(ExitNode(f), exit))
          process(f)
        case Trim.Z(f) =>
          addEdges(
            TransitionEdge(entry, "Trim-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Trim-Exit", exit))
          addEdges(
            ValueEdge(ExitNode(f), exit))
          process(f)
        case Force.Z(xs, vs, f) =>
          xs foreach { declareVariable(entry, _) }
          addEdges(
            TransitionEdge(entry, "Force-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Force-Exit", exit))
          addEdges((xs zip vs) map {
            case (x, v) =>
              val tmp = VariableNode(x, exit.location)
              ValueEdge(ValueNode(v), tmp)
          }: _*)
          addEdges(vs.flatMap(e => Seq(UseEdge(ValueNode(e), entry),
              UseEdge(ValueNode(e), exit))): _*)
          addEdges(ValueEdge(ExitNode(f), exit))
          process(f)
        case Resolve.Z(futures, e) =>
          addEdges(
            TransitionEdge(entry, "Resolve-Enter", EntryNode(e)),
            TransitionEdge(ExitNode(e), "Resolve-Exit", exit))
          addEdges(futures.flatMap(e => Seq(UseEdge(ValueNode(e), entry),
              UseEdge(ValueNode(e), exit))): _*)
          addEdges(ValueEdge(ExitNode(e), exit))
          process(e)
        case IfDef.Z(b, f, g) =>
          addEdges(
            TransitionEdge(entry, "IfDef-Def", EntryNode(f)),
            TransitionEdge(entry, "IfDef-Not", EntryNode(g)),
            TransitionEdge(ExitNode(f), "IfDef-L", exit),
            TransitionEdge(ExitNode(g), "IfDef-R", exit))
          addEdges(
            UseEdge(ValueNode(b), entry),
            UseEdge(ValueNode(b), exit),
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit))
          process(f)
          process(g)
        case Call.Z(target, args, _) =>
          val trans = (e: @unchecked) match {
            case _: CallDef.Z => "CallDef"
            case _: CallSite.Z => "CallSite"
          }
          addEdges(AfterEdge(entry, trans, exit))
          addEdges(args.map(e => UseEdge(ValueNode(e), entry)): _*)
          addEdges(args.map(e => UseEdge(ValueNode(e), exit)): _*)
          addEdges(
              UseEdge(ValueNode(target), entry),
              UseEdge(ValueNode(target), exit))
        case DeclareCallables.Z(callables, body) =>
          callables.map(_.name) foreach { declareVariable(entry, _) }

          for (callable <- callables) {
            val graph = new FlowGraph(callable.body, Some(callable))
            // TODO: Consider computing the subgraph only as needed using the AnalysisCache
            val me = CallableNode(callable, graph)
            addEdges(ValueEdge(me, VariableNode(callable.name, callable +: callable.parents)))
          }

          addEdges(
            TransitionEdge(entry, "Declare-Enter", EntryNode(body)),
            TransitionEdge(ExitNode(body), "Declare-Exit", exit))
          addEdges(ValueEdge(ExitNode(body), exit))
          process(body)
        case e @ Constant.Z(c) =>
          addEdges(
            TransitionEdge(entry, "Const", exit))
          addEdges(
            ValueEdge(ValueNode(e), exit))
        case v: BoundVar.Z =>
          addEdges(
            TransitionEdge(entry, "Var", exit))
          addEdges(
            ValueEdge(ValueNode(v), exit))
        case DeclareType.Z(_, _, body) =>
          addEdges(
            TransitionEdge(entry, "", EntryNode(body)),
            TransitionEdge(ExitNode(body), "", exit))
          addEdges(ValueEdge(ExitNode(body), exit))
          process(body)
        case HasType.Z(body, _) =>
          addEdges(
            TransitionEdge(entry, "", EntryNode(body)),
            TransitionEdge(ExitNode(body), "", exit))
          addEdges(ValueEdge(ExitNode(body), exit))
          process(body)
        case UnboundVar.Z(s) =>
          addEdges(
            AfterEdge(entry, "UnboundVar", exit))
      }
    }

    // Always include arguments to this function.
    nodeSet ++= arguments

    process(root)
  }

  override def hashCode() = root.hashCode ^ (location.hashCode * 37)

  override def equals(o: Any) = o match {
    case f: FlowGraph => root == f.root && location == f.location
    case _ => false
  }

  override def toString() = {
    s"FlowGraph(${shortString(root)}, ${location})"
  }

  override def graphLabel: String = shortString(location.map(_.value).getOrElse(""))

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (if (n == entry) Map("color" -> "green", "peripheries" -> "2") else Map()) ++
      (if (n == exit) Map("color" -> "red", "peripheries" -> "2") else Map()) ++
      (n match {
        case v @ ValueNode(_, _) if arguments contains v =>
          Map("color" -> "green", "peripheries" -> "2")
        case _ => Map()
      })
  }
  override def computedEdgeDotAttributes(n: Edge): DotAttributes = {
    /*
      (e.to match {
        case CallableNode(_, g) =>
          Seq("lhead" -> idFor("cluster", g.root))
        case _ => Seq()
      }) ++
      (e.from match {
        case CallableNode(_, g) =>
          Seq("ltail" -> idFor("cluster", g.root))
        case _ => Seq()
      })*/
    Map()
  }
}

object FlowGraph extends AnalysisRunner[(Expression.Z, Option[Callable.Z]), FlowGraph] {
  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Callable.Z])): FlowGraph = {
    new FlowGraph(params._1, params._2)
  }

  /////// Nodes

  sealed abstract class Node extends WithDotAttributes with PrecomputeHashcode {
    this: Product =>
    val ast: NamedAST
    override def toString() = s"$productPrefix(${shortString(ast)}#${ast.hashCode().formatted("%x")}${System.identityHashCode(ast).formatted("%x")})"

    def group: AnyRef = ast
    def shape: String = "ellipse"
    def color: String = "black"
    def label: String = toString()
    def dotAttributes = Map(
      "label" -> label,
      "shape" -> shape,
      "color" -> color)
  }

  sealed trait WithSpecificAST extends Node {
    this: Product =>
    val location: NamedAST.Z
    val ast = location.value
    override lazy val group = {
      val p = location.parents
      val i = p.lastIndexWhere(_.isInstanceOf[Callable.Z])
      if (i >= 0) {
        p(i)
      } else {
        "Top-level"
      }
    }
  }

  case object EverywhereNode extends Node {
    override def toString() = s"$productPrefix"
    val ast: NamedAST = CallSite(Constant(EverywhereNode), List(), None)
    override val color = "red"
  }

  sealed trait TokenFlowNode extends Node with WithSpecificAST {
    this: Product =>
    val location: Expression.Z
    override def label = location.value match {
      case Future(_) => s"◊"
      case _ => s"${shortString(ast)}"
    }
  }

  sealed trait ValueFlowNode extends Node {
    this: Product =>
    override def shape = "box"
    override def label = s"${shortString(ast)}"
    override def group: AnyRef = this
  }

  // TODO: I think this may not be needed, however it's not clear where to store the nested flowgraph if callables are just VariableNodes.
  case class CallableNode(location: Callable.Z, flowgraph: FlowGraph) extends Node with ValueFlowNode with WithSpecificAST {
    override def equals(o: Any) = o match {
      case o: CallableNode =>
        location == o.location // Ignore flowgraph for equality. This is an optimization.
      case _ =>
        false
    }
  }

  // This stores the class of the value along with it so that nodes with equal values of different types (2.0 == 2) are not collapsed into a single node.
  case class ValueNode(ast: Constant, cls: Option[Class[_]]) extends Node with ValueFlowNode {
    override def label = ast.toString()
  }
  object ValueNode {
    def apply(ast: Argument.Z): ValueFlowNode = ast match {
      case a: Constant.Z =>
        ValueNode(a.value, Option(a.constantValue).map(_.getClass()))
      case a: UnboundVar.Z =>
        throw new IllegalArgumentException(s"Congrats!!! You just volunteered to implement unbound variables in this analysis if you think we really need them: $a")
      case v: BoundVar.Z =>
        VariableNode(v.value, v.parents)
    }
  }

  case class VariableNode(ast: BoundVar, binder: NamedAST.Z) extends Node with ValueFlowNode {
    assert(binder.parents.tail.count(_.boundVars contains ast) == 0)
    require(binder.boundVars contains ast)

    override def toString() = s"$productPrefix(${shortString(ast)}#${ast.asInstanceOf[AnyRef].hashCode().formatted("%x")}${System.identityHashCode(ast).formatted("%x")}, ${binder.value})"

    override def label = binder match {
      case Force.Z(_, _, _) => s"♭ $ast"
      case _ => s"$ast from ${shortString(binder.value)}"
    }
  }
  object VariableNode {
    def apply(ast: BoundVar, path: Seq[NamedAST.Z]): VariableNode = {
      assert(path.count(_.boundVars contains ast) <= 1)
      VariableNode(ast, path.find(_.boundVars contains ast).getOrElse(
        throw new IllegalArgumentException(s"$ast should be a variable bound on the path:\n${path.head}")))
    }
  }

  /*
  case class FutureFieldNode(location: New.Z, field: Field) extends Node with ValueFlowNode with WithSpecificAST {
    override def label = s"◊ $field"
  }
  case class ArgumentFieldNode(location: New.Z, field: Field) extends Node with ValueFlowNode with WithSpecificAST {
    override def label = s"$field"
  }
  */

  case class EntryNode(location: Expression.Z) extends Node with TokenFlowNode {
    override def label = s"⤓ ${super.label}"
  }

  case class ExitNode(location: Expression.Z) extends Node with TokenFlowNode {
    override def label = s"↧ ${super.label}"
  }

  /////// Edges

  sealed abstract class Edge extends WithDotAttributes with PrecomputeHashcode with EdgeBase[Node] {
    this: Product =>
    val from: Node
    val to: Node

    def style: String
    def color: String = "black"
    def label: String
    def dotAttributes = Map(
      "label" -> label,
      "style" -> style,
      "color" -> color)
  }

  // Control flow edges
  sealed trait HappensBeforeEdge extends Edge {
    this: Product =>

  }
  sealed trait TokenFlowEdge extends HappensBeforeEdge {
    this: Product =>
  }

  case class TransitionEdge(from: Node, trans: String, to: Node) extends Edge with TokenFlowEdge {
    override def style: String = "solid"
    override def label = trans
  }
  case class AfterHaltEdge(from: Node, trans: String, to: Node) extends Edge with HappensBeforeEdge {
    override def style: String = "solid"
    override def color: String = "grey"
    override def label = trans
  }
  case class AfterEdge(from: Node, trans: String, to: Node) extends Edge with HappensBeforeEdge {
    override def style: String = "solid"
    override def color: String = "grey"
    override def label = trans
  }

  // TODO: Determine exactly how this is different from ValueEdges and document it. The main difference at the moment is that you can have many UseEdges, but onlye one value edge.
  /** A node uses a value without directly publishing it or taking that value.
   */
  case class UseEdge(from: Node, to: Node) extends Edge {
    override def style: String = "dotted"
    override def color: String = "grey"
    override def label = "‣"
  }

  /** A flow edge that skips over futures and objects.
    *
    * If the source is an ExitNode then the input value is a future, if it
    * is a ValueNode then the input value is a bare value. The input value
    * is never an object.
    *
    * These edges is only added by CallGraph.
    */
  case class FutureValueSourceEdge(from: Node, to: TokenFlowNode) extends Edge {
    override def style: String = "dotted"
    override def color: String = "blue"
    override def label = "⤸"
  }

  // Value flow edges
  sealed trait ValueFlowEdge extends Edge {
    this: Product =>
    override def style: String = "dashed"
  }

  case class ValueEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def label = "▾"
  }
}
