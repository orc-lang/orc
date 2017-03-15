package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import scala.reflect.ClassTag
import orc.compile.Logger

/** A control flow graph for an Orc program which represents the flow of tokens through the program.
  *
  * By extension this also represents the flow of publications. This graph does not represent
  * flow due to halting since no token flows from the LHS of otherwise to the RHS.
  *
  */
class FlowGraph(val root: Expression) {
  import FlowGraph._

  def compute(): (Set[Node], Set[Edge]) = {
    import scala.collection.mutable
    val nodes = mutable.HashSet[Node]()
    val edges = mutable.HashSet[Edge]()

    implicit class NodeAdds(n: Node) {
      def outEdges = edges.filter(_.from == n).toSet
      def inEdges = edges.filter(_.to == n).toSet
      def outEdgesOf[T <: Edge: ClassTag] = {
        val TType = implicitly[ClassTag[T]]
        edges.collect { case TType(e) if e.from == n => e }.toSet
      }
      def inEdgesOf[T <: Edge: ClassTag] = {
        val TType = implicitly[ClassTag[T]]
        edges.collect { case TType(e) if e.to == n => e }.toSet
      }
    }

    def traceDefValue(v: Node): Option[Set[Def]] = v match {
      case CallableNode(d: Def) =>
        Logger.fine(s"Traced to def: $d")
        Some(Set(d))
      case ExitNode(_: CallSite) => None
      case _: EntryNode =>
        throw new AssertionError()
      case v =>
        Logger.fine(s"tracing back though: $v")
        val ins = v.inEdgesOf[ValueFlowEdge].map(_.from)
        Logger.fine(s"Found ins: $ins")
        val traces = ins.map(traceDefValue)
        if (traces contains None) {
          None
        } else {
          Some(traces.flatten.flatten)
        }
    }

    def process(e: Expression, path: List[Expression]): Unit = {
      implicit val pathHere = path

      def recurse(e1: Expression) = process(e1, e :: path)
      def touchArgument(a: Argument) = {
        nodes += ValueNode(a)
      }
      def declareVariable(e: Node, a: BoundVar) = {
        touchArgument(a)
        edges += DefEdge(e, ValueNode(a))
      }

      val entry = EntryNode(e)
      val exit = ExitNode(e)

      nodes ++= Set(entry, exit)

      e match {
        case Branch(f, x, g) =>
          declareVariable(entry, x)
          edges ++= Seq(
            TransitionEdge(entry, "Bra-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Bra-PubL", EntryNode(g)),
            TransitionEdge(ExitNode(g), "Bra-PubR", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(f), ValueNode(x)),
            ValueEdge(ExitNode(g), exit))
          recurse(f)
          recurse(g)
        case Parallel(f, g) =>
          edges ++= Seq(
            TransitionEdge(entry, "Par-Enter", EntryNode(f)),
            TransitionEdge(entry, "Par-Enter", EntryNode(g)),
            TransitionEdge(ExitNode(f), "Par-PubL", exit),
            TransitionEdge(ExitNode(g), "Par-PubR", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit))
          recurse(f)
          recurse(g)
        case Future(f) =>
          edges ++= Seq(
            TransitionEdge(entry, "Future-Spawn", EntryNode(f)),
            TransitionEdge(entry, "Future-Future", exit))
          edges ++= Seq(
            FutureEdge(ExitNode(f), exit))
          recurse(f)
        case Force(xs, vs, b, f) =>
          xs foreach { declareVariable(entry, _) }
          vs foreach { touchArgument(_) }
          edges ++= Seq(
            TransitionEdge(entry, "Force-Enter", EntryNode(f)),
            TransitionEdge(ExitNode(f), "Force-Exit", exit))
          edges ++= (xs zip vs) map { case (x, v) => ForceEdge(ValueNode(v), b, ValueNode(x)) }
          edges ++= vs.map(e => UseEdge(ValueNode(e), entry)) :+
            ValueEdge(ExitNode(f), exit)
          recurse(f)
        case IfDef(b, f, g) =>
          touchArgument(b)
          edges ++= Seq(
            TransitionEdge(entry, "IfDef-Def", EntryNode(f)),
            TransitionEdge(entry, "IfDef-Not", EntryNode(g)),
            TransitionEdge(ExitNode(f), "IfDef-L", exit),
            TransitionEdge(ExitNode(g), "IfDef-R", exit))
          edges ++= Seq(
            UseEdge(ValueNode(b), entry),
            ValueEdge(ExitNode(f), exit),
            ValueEdge(ExitNode(g), exit))
          recurse(f)
          recurse(g)
        case CallSite(target, args, _) =>
          touchArgument(target)
          args foreach touchArgument
          edges ++= Seq(
            TransitionEdge(entry, "CallSite", exit))
          edges ++= args.map(e => UseEdge(ValueNode(e), entry)) :+
            UseEdge(ValueNode(target), entry)
        case CallDef(target, args, _) =>
          touchArgument(target)
          args foreach touchArgument
          traceDefValue(ValueNode(target)) match {
            case Some(s) if s.isEmpty =>
              Logger.fine(s"Found that calldef does not call a def: $e")
            case Some(s) =>
              Logger.fine(s"Found that calldef calls one of: ${s.mkString("\n")}")
              for (d <- s) {
                edges ++= (d.formals zip args).map { case (formal, arg) => ValueEdge(ValueNode(arg), ValueNode(formal)) }
                edges ++= Seq(
                  TransitionEdge(entry, "CallDef", EntryNode(d.body)),
                  TransitionEdge(ExitNode(d.body), "ReturnDef", exit),
                  ValueEdge(ExitNode(d.body), exit))
              }
            case None =>
              Logger.fine(s"May call any def or anything else.")
              edges ++= Seq(
                TransitionEdge(entry, "??????", exit))
          }
          edges ++= args.map(e => UseEdge(ValueNode(e), entry)) :+
            UseEdge(ValueNode(target), entry)
        case DeclareCallables(callables, body) =>
          callables.map(_.name) foreach { declareVariable(entry, _) }

          for (callable <- callables) {
            callable.formals foreach { declareVariable(entry, _) }
            val me = CallableNode(callable)
            nodes += me
            edges += ValueEdge(me, ValueNode(callable.name))
            recurse(callable.body)
          }

          edges ++= Seq(
            TransitionEdge(entry, "Declare-Enter", EntryNode(body)),
            TransitionEdge(ExitNode(body), "Declare-Exit", exit))
          edges += ValueEdge(ExitNode(body), exit)
          recurse(body)
        case e @ Constant(c) =>
          touchArgument(e)
          edges ++= Seq(
            TransitionEdge(entry, "Const", exit))
          edges ++= Seq(
            ValueEdge(ValueNode(e), exit))
        case v: BoundVar =>
          edges ++= Seq(
            TransitionEdge(entry, "Var", exit))
          edges ++= Seq(
            ValueEdge(ValueNode(v), exit))
      }
    }

    process(root, List())

    (nodes.toSet, edges.toSet)
  }

  val (nodes, edges) = compute()

  def toDot(): String = {
    import scala.collection.mutable
    val idMap = mutable.HashMap[AnyRef, String]()
    var prevID = 0
    def idFor(s: String, o: AnyRef) = idMap.getOrElseUpdate(o, {
      prevID += 1
      s"$s$prevID"
    })
    val nodesStr = {
      nodes.groupBy(_.group).map {
        case (e, ns) if ns.size == 1 =>
          val n = ns.head
          s"""${idFor("n", n)} ${n.dotAttributeString};"""
        case (e, ns) =>
          s"""subgraph ${idFor("nocluster", e)} {
label="${TokenFlowGraph.shortString(e)}";
${ns.map(n => s"""${idFor("n", n)} ${n.dotAttributeString};""").mkString("\n")}
}"""
      }.mkString("\n")
    }
    s"""
digraph {
$nodesStr
${edges.map(e => s"""${idFor("n", e.from)} -> ${idFor("n", e.to)} ${e.dotAttributeString};""").mkString("\n")}
}
"""
  }

  def debugShow(): Unit = {
    import java.io.File
    import scala.sys.process._
    import java.io.ByteArrayInputStream
    import java.nio.charset.StandardCharsets
    val tmp = File.createTempFile("orcprog", ".svg");
    tmp.deleteOnExit();

    val data = new ByteArrayInputStream(toDot.getBytes(StandardCharsets.UTF_8));
    ("dot -Tpng /dev/stdin -o/dev/stdout" #< data #> tmp).!
    Seq("display", tmp.getAbsolutePath()).!
  }
}

object FlowGraph {
  def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"

  trait DotAttributes {
    def dotAttributes: Map[String, String]

    def dotAttributeString = {
      s"[${dotAttributes.map(p => s"${p._1}=${'"'}${p._2}${'"'}").mkString(",")}]"
    }
  }

  sealed abstract class Node extends DotAttributes {
    this: Product =>
    val e: NamedAST
    override def toString() = s"$productPrefix(${shortString(e)})"

    def group: AnyRef = e
    def shape: String = "ellipse"
    def color: String = "black"
    def label: String = toString()
    def dotAttributes = Map(
      "label" -> label,
      "shape" -> shape,
      "color" -> color)
  }

  trait TokenFlowNode extends Node {
    this: Product =>
  }
  trait ValueFlowNode extends Node {
    this: Product =>
    override def shape = "box"
    override def label = e.toString()
    override def group: AnyRef = this
  }

  case class CallableNode(e: Callable) extends Node with ValueFlowNode {
  }
  case class ValueNode(e: Argument) extends Node with ValueFlowNode {
  }

  case class EntryNode(e: Expression)(implicit path: List[Expression]) extends Node with TokenFlowNode {
    override def label = s"entry ${shortString(e)}"
  }

  case class ExitNode(e: Expression)(implicit path: List[Expression]) extends Node with TokenFlowNode {
    override def label = s"exit ${shortString(e)}"
  }

  sealed abstract class Edge extends DotAttributes {
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

  trait TokenFlowEdge extends Edge {

  }
  trait ValueFlowEdge extends Edge {
    override def style: String = "dashed"
  }
  trait DefUseEdge extends Edge {
    override def style: String = "dotted"
    override def color: String = "grey"
  }

  case class TransitionEdge(from: Node, trans: String, to: Node) extends Edge with TokenFlowEdge {
    override def style: String = "solid"
    override def label = trans
  }

  case class UseEdge(from: ValueNode, to: Node) extends Edge with DefUseEdge {
    override def label = "use"
  }
  case class DefEdge(from: Node, to: ValueNode) extends Edge with DefUseEdge {
    override def label = "def"
  }

  case class ValueEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def label = ""
  }
  case class FutureEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def color: String = "green"
    override def label = "◊"
  }
  case class ForceEdge(from: ValueNode, publishForce: Boolean, to: ValueNode) extends Edge with ValueFlowEdge {
    override def color: String = "red"
    override def label = "♭" + (if (publishForce) "p" else "c")
  }

}
