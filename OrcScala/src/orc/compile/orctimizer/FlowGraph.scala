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

import FlowGraph._
import orc.compile.flowanalysis.GraphDataProvider
import orc.compile.flowanalysis.EdgeBase
import orc.util.DotUtils._
import orc.compile.flowanalysis.DebuggableGraphDataProvider

/** A control flow graph for an Orc program which represents the flow of tokens through the program.
  *
  * By extension this also represents the flow of publications. This graph does not represent
  * flow due to halting since no token flows from the LHS of otherwise to the RHS.
  *
  */
class FlowGraph(val root: Expression, val location: Option[SpecificAST[Callable]] = None) extends DebuggableGraphDataProvider[Node, Edge] {
  outer =>

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

  protected[this] def addEdges(es: Edge*): Unit = {
    for (e <- es) {
      edgeSet += e
      nodeSet += e.to
      nodeSet += e.from
    }
  }

  val entry = EntryNode(SpecificAST(root, location.subtreePath))
  val exit = ExitNode(SpecificAST(root, location.subtreePath))

  def arguments: List[VariableNode] = {
    location.map(l => l.ast.formals.map(VariableNode(_, l.ast :: l.path))).getOrElse(List())
  }

  protected[this] def compute(): Unit = {
    def process(e: Expression, path: List[NamedAST]): Unit = {
      val potentialPath = e :: path
      def recurse(e1: Expression) = process(e1, potentialPath)

      def astInScope[T <: NamedAST](ast: T): SpecificAST[T] = {
        val i = potentialPath.indexWhere(_.subtrees.toSet contains ast)
        if (i < 0) {
          assert(ast == potentialPath.last, s"Path ${path.map(shortString).mkString("[", ", ", "]")} does not contain a parent of $ast")
          SpecificAST(ast, List())
        } else {
          //Logger.fine(s"Found index $i in ${potentialPath.map(shortString).mkString("[", ", ", "]")} looking for $ast")
          SpecificAST(ast, potentialPath.drop(i))
        }
      }

      def declareVariable(e: Node, a: BoundVar) = {
        //addEdges(DefEdge(e, ValueNode(a)))
      }

      val entry = EntryNode(astInScope(e))
      val exit = ExitNode(astInScope(e))

      // Add nodes that we process even if they don't have edges
      nodeSet ++= Set(entry, exit)

      e match {
        case Stop() =>
          ()
        case FieldAccess(a, f) =>
          addEdges(
            ValueEdge(ValueNode(a, potentialPath), exit),
            TransitionEdge(entry, "FieldAccess", exit))
        case nw @ New(self, selfT, bindings, objT) =>
          declareVariable(entry, self)
          addEdges(
            ValueEdge(exit, VariableNode(self, potentialPath)))
          addEdges(
            TransitionEdge(entry, "New-Obj", exit))
          for ((f, b) <- bindings) {
            b match {
              case FieldFuture(e) =>
                //Logger.fine(s"Processing field $b with $potentialPath")
                val se = SpecificAST(e, b :: potentialPath)
                val tmp = FutureFieldNode(astInScope(nw), f)
                addEdges(
                  TransitionEdge(entry, "New-Spawn", EntryNode(se)),
                  ValueEdge(ExitNode(se), tmp),
                  ValueEdge(tmp, exit))
                process(e, b :: potentialPath)
              case FieldArgument(v) =>
                val tmp = ArgumentFieldNode(astInScope(nw), f)
                addEdges(
                  ValueEdge(ValueNode(v, potentialPath), tmp),
                  ValueEdge(tmp, exit))
            }
          }
        case Branch(f, x, g) =>
          declareVariable(entry, x)
          addEdges(
            TransitionEdge(entry, "Bra-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Bra-PubL", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(g)), "Bra-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(astInScope(f)), VariableNode(x, exit.location)),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Otherwise(f, g) =>
          addEdges(
            TransitionEdge(entry, "Otw-Entry", EntryNode(astInScope(f))),
            AfterHaltEdge(entry, "Otw-Halt", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "Otw-PubL", exit),
            TransitionEdge(ExitNode(astInScope(g)), "Otw-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Parallel(f, g) =>
          addEdges(
            TransitionEdge(entry, "Par-Enter", EntryNode(astInScope(f))),
            TransitionEdge(entry, "Par-Enter", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "Par-PubL", exit),
            TransitionEdge(ExitNode(astInScope(g)), "Par-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Future(f) =>
          addEdges(
            TransitionEdge(entry, "Future-Spawn", EntryNode(astInScope(f))),
            TransitionEdge(entry, "Future-Future", exit))
          addEdges(
            ValueEdge(ExitNode(astInScope(f)), exit))
          recurse(f)
        case Trim(f) =>
          addEdges(
            TransitionEdge(entry, "Trim-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Trim-Exit", exit))
          addEdges(
            ValueEdge(ExitNode(astInScope(f)), exit))
          recurse(f)
        case Force(xs, vs, b, f) =>
          xs foreach { declareVariable(entry, _) }
          addEdges(
            TransitionEdge(entry, "Force-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Force-Exit", exit))
          addEdges((xs zip vs) map {
            case (x, v) =>
              val tmp = VariableNode(x, exit.location)
              ValueEdge(ValueNode(v, potentialPath), tmp)
          }: _*)
          addEdges(vs.map(e => UseEdge(ValueNode(e, potentialPath), entry)): _*)
          addEdges(ValueEdge(ExitNode(astInScope(f)), exit))
          recurse(f)
        case IfDef(b, f, g) =>
          addEdges(
            TransitionEdge(entry, "IfDef-Def", EntryNode(astInScope(f))),
            TransitionEdge(entry, "IfDef-Not", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "IfDef-L", exit),
            TransitionEdge(ExitNode(astInScope(g)), "IfDef-R", exit))
          addEdges(
            UseEdge(ValueNode(b, potentialPath), entry),
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Call(target, args, _) =>
          val trans = (e: @unchecked) match {
            case _: CallDef => "CallDef"
            case _: CallSite => "CallSite"
          }
          addEdges(AfterEdge(entry, trans, exit))
          addEdges(args.map(e => UseEdge(ValueNode(e, potentialPath), entry)): _*)
          addEdges(UseEdge(ValueNode(target, potentialPath), entry))
        case DeclareCallables(callables, body) =>
          callables.map(_.name) foreach { declareVariable(entry, _) }

          for (callable <- callables) {
            val loc = SpecificAST(callable, potentialPath)
            val graph = new FlowGraph(callable.body, Some(loc))
            val me = CallableNode(loc, graph)
            addEdges(ValueEdge(me, VariableNode(callable.name, potentialPath)))
          }

          addEdges(
            TransitionEdge(entry, "Declare-Enter", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "Declare-Exit", exit))
          addEdges(ValueEdge(ExitNode(astInScope(body)), exit))
          recurse(body)
        case e @ Constant(c) =>
          addEdges(
            TransitionEdge(entry, "Const", exit))
          addEdges(
            ValueEdge(ValueNode(e), exit))
        case v: BoundVar =>
          addEdges(
            TransitionEdge(entry, "Var", exit))
          addEdges(
            ValueEdge(ValueNode(v, potentialPath), exit))
        case DeclareType(_, _, body) =>
          addEdges(
            TransitionEdge(entry, "", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "", exit))
          addEdges(ValueEdge(ExitNode(astInScope(body)), exit))
          recurse(body)
        case HasType(body, _) =>
          addEdges(
            TransitionEdge(entry, "", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "", exit))
          addEdges(ValueEdge(ExitNode(astInScope(body)), exit))
          recurse(body)
        case UnboundVar(s) =>
          addEdges(
            AfterEdge(entry, "UnboundVar", exit))
      }
    }

    // Always include arguments to this function.
    nodeSet ++= arguments

    process(root, location.subtreePath)
  }

  override def hashCode() = root.hashCode ^ (location.hashCode * 37)

  override def equals(o: Any) = o match {
    case f: FlowGraph => root == f.root && location == f.location
    case _ => false
  }

  override def toString() = {
    s"FlowGraph(${shortString(root)}, ${location})"
  }

  override def graphLabel: String = shortString(location.map(_.ast).getOrElse(""))

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    (if (n == entry) Map("color" -> "green", "peripheries" -> "2") else Map()) ++
      (if (n == exit) Map("color" -> "red", "peripheries" -> "2") else Map()) ++
      (n match {
        case v @ ValueNode(_) if arguments contains v =>
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

object FlowGraph {
  /////// Nodes

  sealed abstract class Node extends WithDotAttributes with PrecomputeHashcode {
    this: Product =>
    val ast: NamedAST
    override def toString() = s"$productPrefix(${shortString(ast)})"

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
    val location: SpecificAST[NamedAST]
    val ast = location.ast
    override lazy val group = {
      val p = location.path
      val i = p.lastIndexWhere(_.isInstanceOf[Callable])
      if (i >= 0) {
        p(i)
      } else {
        "Top-level"
      }
    }
  }

  sealed trait TokenFlowNode extends Node with WithSpecificAST {
    this: Product =>
    val location: SpecificAST[Expression]
    override def label = location.ast match {
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

  // FIXME: I think this may not be needed, however it's not clear where to store the nested flowgraph if callables are just VariableNodes.
  case class CallableNode(location: SpecificAST[Callable], flowgraph: FlowGraph) extends Node with ValueFlowNode with WithSpecificAST {
  }

  case class ValueNode(ast: Constant) extends Node with ValueFlowNode {
    override def label = ast.toString()
  }
  object ValueNode {
    def apply(ast: Argument, path: List[NamedAST]): ValueFlowNode = ast match {
      case a: Constant =>
        ValueNode(a)
      case a: UnboundVar =>
        throw new IllegalArgumentException(s"Congrats!!! You just volunteered to implement unbound variables in this analysis if you think we really need them.")
      case v: BoundVar =>
        VariableNode(v, path)
    }
  }

  case class VariableNode(ast: BoundVar, binder: NamedAST) extends Node with ValueFlowNode {
    require(binder.boundVars contains ast)

    override def label = binder match {
      case Force(_, _, publishForce, _) => s"♭${if (publishForce) "p" else "c"} $ast"
      case _ => s"$ast from ${shortString(binder)}"
    }
  }
  object VariableNode {
    def apply(ast: BoundVar, path: List[NamedAST]): VariableNode = {
      VariableNode(ast, path.find(_.boundVars contains ast).getOrElse(
        throw new IllegalArgumentException(s"$ast should be a variable bound on the path:\n$path")))
    }
  }

  case class FutureFieldNode(location: SpecificAST[New], field: Field) extends Node with ValueFlowNode with WithSpecificAST {
    override def label = s"◊ $field"
  }
  case class ArgumentFieldNode(location: SpecificAST[New], field: Field) extends Node with ValueFlowNode with WithSpecificAST {
    override def label = s"$field"
  }

  case class EntryNode(location: SpecificAST[Expression]) extends Node with TokenFlowNode {
    override def label = s"⤓ ${super.label}"
  }

  case class ExitNode(location: SpecificAST[Expression]) extends Node with TokenFlowNode {
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

  // Def/use chains
  sealed trait DefUseEdge extends Edge {
    this: Product =>
    override def style: String = "dotted"
    override def color: String = "grey"
  }

  case class UseEdge(from: ValueFlowNode, to: Node) extends Edge with DefUseEdge {
    override def label = "" // ‣
  }

  // Value flow edges
  sealed trait ValueFlowEdge extends Edge {
    this: Product =>
    override def style: String = "dashed"
  }

  case class ValueEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def label = ""
  }

  /*
  case class ProvideFieldEdge(from: Node, field: Field, to: Node) extends Edge with ValueFlowEdge {
    override def color: String = "green"
    override def label = field.toString()
  }
  case class AccessFieldEdge(from: Node, field: Field, to: Node) extends Edge with ValueFlowEdge {
    override def color: String = "red"
    override def label = field.toString()
  }
  case class FutureEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def color: String = "green"
    override def label = "◊"
  }
  case class ForceEdge(from: ValueNode, publishForce: Boolean, to: ValueNode) extends Edge with ValueFlowEdge {
    override def color: String = "red"
    override def label = "♭" + (if (publishForce) "p" else "c")
  }
  */

}
