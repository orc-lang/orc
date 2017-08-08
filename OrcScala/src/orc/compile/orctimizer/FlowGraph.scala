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
import orc.ast.orctimizer.named.IfLenientMethod
import java.lang.IllegalArgumentException

/** A control flow graph for an Orc program which represents the flow of tokens through the program.
  *
  */
class FlowGraph(val root: Expression.Z, val location: Option[Method.Z] = None) extends DebuggableGraphDataProvider[FlowGraph.Node, FlowGraph.Edge] {
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

  def subflowgraphs = nodes.collect({ case MethodNode(_, g) => g }).toSet
  def subgraphs = subflowgraphs
  def allflowgraphs: Set[FlowGraph] = nodes.collect({ case MethodNode(_, g) => g.allflowgraphs }).flatten.toSet + this

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
      val entry = EntryNode(e)
      val exit = ExitNode(e)

      // Add nodes that we process even if they don't have edges
      nodeSet ++= Set(entry, exit)
      
      addEdges(EntryExitEdge(e))

      e match {
        case Stop.Z() =>
          ()
        case GetField.Z(a, f) =>
          addEdges(
            ValueEdge(ValueNode(a), exit),
            TransitionEdge(entry, "GetField", exit))
        case GetMethod.Z(a) =>
          addEdges(
            ValueEdge(ValueNode(a), exit),
            TransitionEdge(entry, "GetMethod", exit))
        case nw @ New.Z(self, selfT, bindings, objT) =>
          addEdges(
            ValueEdge(exit, VariableNode(self, nw)))
          addEdges(
            TransitionEdge(entry, "New-Obj", exit))
          for ((f, b) <- bindings) {
            b match {
              case FieldFuture.Z(se) =>
                //Logger.fine(s"Processing field $b with $potentialPath")
                //val tmp = FutureFieldNode(nw, f)
                addEdges(
                  TransitionEdge(entry, "New-Spawn", EntryNode(se)),
                  ValueEdge(ExitNode(se), exit))
                process(se)
              case FieldArgument.Z(v) =>
                addEdges(
                  ValueEdge(ValueNode(v), exit))
            }
          }
        case Branch.Z(f, x, g) =>
          addEdges(
            TransitionEdge(entry, "Bra-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Bra-PubL", EntryNode(g)),
            TransitionEdge(ExitNode(g), "Bra-PubR", exit))
          addEdges(
            ValueEdge(ExitNode(f), VariableNode(x, exit.location)),
            ValueEdge(ExitNode(g), exit))
          addEdges(
            CombinatorInternalOrderEdge(ExitNode(f), "", exit))
          process(f)
          process(g)
        case Otherwise.Z(f, g) =>
          addEdges(
            TransitionEdge(entry, "Otw-Entry", EntryNode(f)),
            CombinatorInternalOrderEdge(entry, "Otw-Halt", EntryNode(g)),
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
          addEdges(
            TransitionEdge(entry, "Force-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Force-Exit", exit))
          addEdges((xs zip vs) map {
            case (x, v) =>
              val tmp = VariableNode(x, exit.location)
              ValueEdge(ValueNode(v), tmp)
          }: _*)
          addEdges(
            vs.map(e => ValueEdge(ValueNode(e), entry)): _*)
          addEdges(ValueEdge(ExitNode(f), exit))
          process(f)
        case Resolve.Z(futures, e) =>
          addEdges(
            TransitionEdge(entry, "Resolve-Enter", EntryNode(e)),
            TransitionEdge(ExitNode(e), "Resolve-Exit", exit))
          addEdges(
            futures.map(e => ValueEdge(ValueNode(e), entry)): _*)
          addEdges(ValueEdge(ExitNode(e), exit))
          process(e)
        case IfLenientMethod.Z(b, f, g) =>
          addEdges(
            TransitionEdge(entry, "IfDef-Def", EntryNode(f)),
            TransitionEdge(entry, "IfDef-Not", EntryNode(g)),
            TransitionEdge(ExitNode(f), "IfDef-L", exit),
            TransitionEdge(ExitNode(g), "IfDef-R", exit))
          addEdges(
            ValueEdge(ValueNode(b), entry),
            //ValueEdge(ValueNode(b), exit),
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit)
            )
          process(f)
          process(g)
        case Call.Z(target, args, _) =>
          //addEdges(AfterEdge(entry, exit))
          addEdges(args.map(e => ValueEdge(ValueNode(e), entry)): _*)
          //addEdges(args.map(e => ValueEdge(ValueNode(e), exit)): _*)
          addEdges(
            ValueEdge(ValueNode(target), entry),
            //ValueEdge(ValueNode(target), exit)
            )
        case DeclareMethods.Z(callables, body) =>
          for (callable <- callables) {
            val graph = new FlowGraph(callable.body, Some(callable))
            // TODO: Consider computing the subgraph only as needed using the AnalysisCache
            val me = MethodNode(callable, graph)
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
          // TODO: Eliminate these nodes entirely.
          addEdges(
            TransitionEdge(entry, "", EntryNode(body)),
            TransitionEdge(ExitNode(body), "", exit))
          addEdges(ValueEdge(ExitNode(body), exit))
          process(body)
        case HasType.Z(body, _) =>
          // TODO: Eliminate these nodes entirely.
          addEdges(
            TransitionEdge(entry, "", EntryNode(body)),
            TransitionEdge(ExitNode(body), "", exit))
          addEdges(ValueEdge(ExitNode(body), exit))
          process(body)
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
        case v: ValueNode if arguments contains v =>
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

object FlowGraph extends AnalysisRunner[(Expression.Z, Option[Method.Z]), FlowGraph] {
  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): FlowGraph = {
    new FlowGraph(params._1, params._2)
  }

  /////// Nodes

  /** A flow graph node.
    */
  sealed abstract class Node extends WithDotAttributes with PrecomputeHashcode {
    this: Product =>
    val ast: NamedAST
    override def toString() = s"$productPrefix(${shortString(ast)}#${ast.hashCode().formatted("%x")})"

    def group: AnyRef = ast
    def shape: String = "ellipse"
    def color: String = "black"
    def label: String = toString()
    def dotAttributes = Map(
      "label" -> label,
      "shape" -> shape,
      "color" -> color)
  }

  /** A node which is associated with a specific node in the AST.
    */
  sealed trait WithSpecificAST extends Node {
    this: Product =>
    val location: NamedAST.Z
    val ast: NamedAST = location.value
    override lazy val group = {
      val p = location.parents
      val i = p.lastIndexWhere(_.isInstanceOf[Method.Z])
      if (i >= 0) {
        p(i)
      } else {
        "Top-level"
      }
    }
  }

  /** A node which represents every possible point in the AST.
    *
    * This is used in a punned fashion as various types of nodes.
    */
  case object EverywhereNode extends Node {
    override def toString() = s"$productPrefix"
    val ast: NamedAST = Call(Constant(EverywhereNode), List(), None)
    override val color = "red"
  }

  /** A node through which tokens flow.
   *  
   *  These tokens may still produce values representing their publications.
   *  
   *  These nodes are pinned in the token flow graph, and perform their operations exactly when a token reaches them.
   */
  sealed trait TokenFlowNode extends Node with WithSpecificAST {
    this: Product =>
    val location: Expression.Z
    override def label = location.value match {
      case Future(_) => s"◊"
      case _ => s"${shortString(ast)}"
    }
  }

  /** A node representing a token positioned before an expression.
   *  
   *  These nodes may not produce values.
   */
  case class EntryNode(location: Expression.Z) extends Node with TokenFlowNode {
    override def label = s"⤓ ${super.label}"
  }

  /** A node representing a token positioned after an expression.
   *  
   *  These nodes must produce a value.
   */
  case class ExitNode(location: Expression.Z) extends Node with TokenFlowNode {
    override def label = s"↧ ${super.label}"
  }

  /** A node which provides or processes a value.
   *  
   *  These nodes need not execute at any specific point and in general do not execute at all.
   */
  sealed trait ValueNode extends Node {
    this: Product =>
    override def shape = "box"
    override def label = s"${shortString(ast)}"
    override def group: AnyRef = this
  }

  /** A constructor object for ValueFlowNodes which converts any argument into a node.
   */
  object ValueNode {
    def apply(ast: Argument.Z): ValueNode = ast match {
      case a: Constant.Z =>
        ConstantNode(a.value, Option(a.constantValue).map(_.getClass()))
      case a: UnboundVar.Z =>
        throw new IllegalArgumentException(s"FlowGraphs cannot have unbound variables: $a")
      case v: BoundVar.Z =>
        VariableNode(v.value, v.parents)
    }
  }

  /** A node which represents a constant value.
   */
  case class ConstantNode(ast: Constant, cls: Option[Class[_]]) extends Node with ValueNode {
    // This stores the class of the value along with it so that nodes with equal values of different types (2.0 == 2) are not collapsed into a single node.
    override def label = ast.toString()
  }
  
  /** A node which represents a variable in the program, with its value taken from another value and potentially processed.
   *  
   *  These nodes are used to represent, for example, the output variables of futures. In that case, the input to this node
   *  may be a future and the output will be the value in the future or the input value if it is not a future.
   *  
   *  This has manual case class features.
   */
  sealed class VariableNode(val ast: BoundVar, val binder: NamedAST.Z) extends Node with ValueNode with Product {
    //assert(binder.parents.tail.count(_.boundVars contains ast) == 0)
    //require(binder.boundVars contains ast)

    override def toString() = {
      //s"$productPrefix(${shortString(ast)}#${ast.asInstanceOf[AnyRef].hashCode().formatted("%x")}${System.identityHashCode(ast).formatted("%x")}, ${binder.value})"
      s"$productPrefix(${shortString(ast)}#${ast.asInstanceOf[AnyRef].hashCode().formatted("%x")}, ${shortString(binder.value)})"
    }

    override def label = binder match {
      case Force.Z(_, _, _) => s"♭ $ast"
      case _ => s"$ast from ${shortString(binder.value)}"
    }
    
    override val hashCode = ast.hashCode
    override def canEqual(o: Any): Boolean = o.isInstanceOf[VariableNode]
    override def equals(o: Any): Boolean = o match {
      case o: MethodNode => o == this
      case o: VariableNode => o.ast == ast && o.binder == binder
      case _ => false
    }
    
    def productElement(n: Int): Any = n match {
      case 0 => ast
      case 1 => binder
    }
    def productArity: Int = 2
    override def productPrefix = "VariableNode"
  }
  object VariableNode {
    def apply(ast: BoundVar, path: Seq[NamedAST.Z]): VariableNode = {
      //assert(path.count(_.boundVars contains ast) <= 1)
      VariableNode(ast, path.find(_.boundVars contains ast).getOrElse(
        throw new IllegalArgumentException(s"$ast should be a variable bound on the path:\n${path.head}")))
    }
    
    def apply(ast: BoundVar, binder: NamedAST.Z): VariableNode = {
      new VariableNode(ast, binder)
    }
    
    def unapply(v: VariableNode): Option[(BoundVar, NamedAST.Z)] = {
      if (v == null) {
        None
      } else {
        Some((v.ast, v.binder))
      }
    }
  }

  /** A special variable node for methods.
   *  
   *  This node exists to carry the flowgraph instance. Most users can treat it as a normar VariableNode.
   */
  case class MethodNode(location: Method.Z, flowgraph: FlowGraph) extends { override val ast: BoundVar = location.name } with VariableNode(location.name, location.parent.get) with WithSpecificAST {
    override def equals(o: Any) = o match {
      case o: MethodNode =>
        location == o.location // Ignore flowgraph for equality. This is an optimization.
      case _ =>
        false
    }

    override def productPrefix = "MethodNode"
  }

  /////// Edges

  /** An edge in a flow graph.
   */
  sealed abstract class Edge extends WithDotAttributes with PrecomputeHashcode with EdgeBase[Node] {
    this: Product =>

    /** The source of the edge.
      */
    val from: Node
    /** The destination of the edge.
      */
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
  
  /** Any edge that represents the fact that the a the source must be reached ("executed") before the destination.
   */
  sealed trait HappensBeforeEdge extends Edge {
    this: Product =>

  }
  
  /** Any edge along which tokens flow within Orctimizer.
   *  
   *  These edges represent potential rewrites in the token semantics. However, not all rewrites are
   *  TransitionEdges. TransitionEdge are not used to represent transition which involve executing
   *  actions outside of Orc (such as site calls). TransitionEdge are used for futures reads (which 
   *  depend on an implicit context).
   */
  case class TransitionEdge(from: Node, trans: String, to: Node) extends Edge with HappensBeforeEdge {
    override def style: String = "solid"
    override def label = trans
  }

  /** An ordering edge that does not directly carry tokens.
   *  
   *  This is used to represent such things as calls to external or unknown methods which
   *  may perform any action.
   */
  sealed abstract class AfterEdge extends Edge with HappensBeforeEdge  {
    this: Product =>
      
    override def style: String = "solid"
    override def color: String = "grey"
    override def label = "then"
    
    /*
    override val hashCode = from.hashCode + to.hashCode*31
    override def canEqual(o: Any): Boolean = o.isInstanceOf[AfterEdge]
    override def equals(o: Any): Boolean = o match {
      case o: AfterEdge => o.from == from && o.to == to
      case _ => false
    }
    override def toString() = s"AfterEdge($from, $to)"
    
    def productElement(n: Int): Any = n match {
      case 0 => from
      case 1 => to
    }
    def productArity: Int = 2
    override def productPrefix = "AfterEdge"
    */
  }
  
  /*
  object AfterEdge {
    def apply(from: Node, to: Node): AfterEdge = {
      new AfterEdge(from, to)
    }

    def unapply(v: AfterEdge): Option[(Node, Node)] = {
      if (v != null)
        Some((v.from, v.to))
      else
        None
    }
  }
  */
  
  /** An ordering edge attaching related parts inside an AST nodes graph.
   */
  case class CombinatorInternalOrderEdge(from: Node, trans: String, to: Node) extends AfterEdge {
    override def style: String = "solid"
    override def color: String = "grey"
    override def label = trans
  }

  /*
  /** An ordering edge which specifically states that the destination will execute after the Orctimizer AST node associated with the source is halted.
   *  
   *  The destination node may not execute every time the source halts.
   */
  case class AfterHaltEdge(val from: Node, trans: String, val to: Node) extends AfterEdge {
    override def style: String = "solid"
    override def color: String = "grey"
    override def label = "halt"
  }
  */
  
  /** An ordering edge attaching an entry node to the exit of the same AST.
   *  
   *  Every AST node is associated with such an edge.
   */
  case class EntryExitEdge(val ast: Expression.Z) extends AfterEdge {
    override def style: String = "dashed"
    override def color: String = "lavender"
    override def label = ""
    
    val from = EntryNode(ast)
    val to = ExitNode(ast)
  }

  // Value flow edges

  /** A flow edge that skips over futures and objects.
    *
    * If the source is an ExitNode then the input value is a future, if it
    * is a ValueNode then the input value is a bare value. The input value
    * is never an object.
    *
    * These edges is only added by CallGraph.
    */
  case class FutureValueSourceEdge(from: Node, to: TokenFlowNode) extends Edge {
    require(from.isInstanceOf[ExitNode] || from.isInstanceOf[ValueNode] || from == EverywhereNode)
    
    override def style: String = "dotted"
    override def color: String = "blue"
    override def label = "⤸"
  }

  /** An edge which represents the flow of a value from one node to another.
   *  
   *  Nodes may only have one output value, but they are allowed to have an arbitrary number
   *  of inputs and may distinguish those inputs based on their source (usually based on 
   *  variable names or value as compared to the arguments in the Orctimizer AST node). 
   */
  case class ValueEdge(from: Node, to: Node) extends Edge {
    override def style: String = "dashed"
    override def label = "▾"
  }
  
  // TODO:
  
  /*
  Add a value edge subtype which represents incoming values which will be published
  AND/OR
  Add a value edge subtype which represents incoming values which are arguments; They effect execution, but will not be published directly.
  
  It is difficult to operate without these since it is very hard to tell the difference between ↧ call node values which are arguments and which are returns.
  What about seperating by which node they are attached to: Entry, Exit.
  The problem still exists for Force and Resolve, but those are much easier to handle.
  The split could still be make with those as long as I am smart about flowing timing values across transition edges.
  The entry node would need to have real information on it's output. It would still require another edge: connecting Force Entry with Exit.
  Without that there is no way to get the information since we cannot flow it through the intervening nodes in all cases (Delay).
  Maybe an AfterEdge. It is semantically correct (though not needed) and would be easy to find the other end of.
  
  */
}
