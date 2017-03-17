package orc.compile.orctimizer

import orc.ast.orctimizer.named._
import scala.reflect.ClassTag
import orc.compile.Logger
import orc.ast.PrecomputeHashcode
import orc.values.Field

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

    case class OnAll[T](sel: PartialFunction[Node, T])(proc: T => Unit) {
      def apply(): Unit = {
        /*
        val ns = Set(
            CallableNode(SpecificAST(Site(new BoundVar(Some("All Sites")), List(), Stop(), List(), None, None), Nil)),
            CallableNode(SpecificAST(Def(new BoundVar(Some("All Defs")), List(), Stop(), List(), None, None), Nil))
            )

        val Some(n) = ns.find(sel.isDefinedAt(_))
        val CallableNode(callable@SpecificAST(Callable(_, _, body, _, _, _), _)) = n
        val bodyEntry = EntryNode(SpecificAST(body, callable :: callable.path))
        val bodyExit = ExitNode(SpecificAST(body, callable :: callable.path))
        nodes ++= Seq(bodyEntry, bodyExit)
        edges ++= Seq(TransitionEdge(bodyEntry, "Anything", bodyExit))
        val v = ns.collect(sel).head
        proc(v)
        */
        nodes.collect(sel) foreach proc
      }
    }

    val onAllList = mutable.Set[OnAll[_]]()

    implicit class NodeAdds(n: Node) {
      def outEdges = edges.filter(_.from == n).toSet
      def inEdges = edges.filter(_.to == n).toSet
      def outEdgesOf[T <: Edge: ClassTag] = {
        val TType = implicitly[ClassTag[T]]
        edges.collect { case TType(e) if e.from == n => e }.toSet
      }
      def inEdgesOf[T <: Edge: ClassTag] = {
        val TType = implicitly[ClassTag[T]]
        // TODO: Indexes would help with this.
        edges.collect { case TType(e) if e.to == n => e }.toSet
      }

      def traceValue[T](sel: PartialFunction[Node, T]): Option[Set[T]] = {
        sel.lift(n) match {
          case Some(v) =>
            //Logger.fine(s"found value: $v")
            Some(Set(v))
          case None =>
            val ins = n.inEdgesOf[ValueFlowEdge].map(_.from)
            // TODO: This needs to handle FieldEdges specially.
            n match {
              case _: EntryNode =>
                throw new AssertionError("Should never get here")
              case _ if ins.isEmpty =>
                // We have hit a dead end: What kind is it?
                n match {
                  case CallableNode(_) | ValueNode(_: Constant) =>
                    // If the dead end is a concrete value, then we know it's not something interesting because sel didn't match it.
                    Some(Set())
                  case _ =>
                    // Otherwise, we know nothing since this could be a variable or expression which is not bound fully yet.
                    None
                }
              case n =>
                //Logger.fine(s"tracing back though: $n")
                //Logger.fine(s"Found ins: $ins")
                val traces = ins.map(_.traceValue(sel))
                if (traces contains None) {
                  None
                } else {
                  Some(traces.flatten.flatten)
                }
            }
        }
      }
    }

    def connectCall(entry: Node, exit: Node, args: Seq[Argument], callable: SpecificAST[Callable], addReturnValueEdge: Boolean) = {
      val Callable(n, formals, body, _, _, _) = callable.ast
      val bodyEntry = EntryNode(SpecificAST(body, callable :: callable.path))
      val bodyExit = ExitNode(SpecificAST(body, callable :: callable.path))
      edges ++= (formals zip args).map { case (formal, arg) => ValueEdge(ValueNode(arg), ValueNode(formal)) }
      edges ++= Seq(
        TransitionEdge(entry, "Call", bodyEntry),
        TransitionEdge(bodyExit, "Return", exit))
      if (addReturnValueEdge)
        edges += ValueEdge(bodyExit, exit)
    }

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

      def touchArgument(a: Argument) = {
        nodes += ValueNode(a)
      }
      def declareVariable(e: Node, a: BoundVar) = {
        touchArgument(a)
        edges += DefEdge(e, ValueNode(a))
      }

      val entry = EntryNode(astInScope(e))
      val exit = ExitNode(astInScope(e))

      nodes ++= Set(entry, exit)

      e match {
        case Stop() =>
          ()
        case FieldAccess(a, f) =>
          edges ++= Seq(
              AccessFieldEdge(ValueNode(a), f, exit),
              TransitionEdge(entry, "FieldAccess", exit))
        case New(self, selfT, bindings, objT) =>
          declareVariable(entry, self)
          edges ++= Seq(
            ValueEdge(exit, ValueNode(self)))
          edges ++= Seq(
            TransitionEdge(entry, "New-Obj", exit))
          for ((f, b) <- bindings) {
            b match {
              case FieldFuture(e) =>
                Logger.fine(s"Processing field $b with $potentialPath")
                val se = SpecificAST(e, b :: potentialPath)
                edges += TransitionEdge(entry, "New-Spawn", EntryNode(se))
                val tmp = ValueNode(new BoundVar())
                nodes += tmp
                edges += FutureEdge(ExitNode(se), tmp)
                edges += ProvideFieldEdge(tmp, f, exit)
                process(e, b :: potentialPath)
              case FieldArgument(v) =>
                touchArgument(v)
                edges += UseEdge(ValueNode(v), entry)
                edges += ProvideFieldEdge(ValueNode(v), f, exit)
            }
          }
        case Branch(f, x, g) =>
          declareVariable(entry, x)
          edges ++= Seq(
            TransitionEdge(entry, "Bra-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Bra-PubL", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(g)), "Bra-PubR", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(astInScope(f)), ValueNode(x)),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Otherwise(f, g) =>
          edges ++= Seq(
            TransitionEdge(entry, "Otw-Entry", EntryNode(astInScope(f))),
            AfterHaltEdge(entry, "Otw-Halt", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "Otw-PubL", exit),
            TransitionEdge(ExitNode(astInScope(g)), "Otw-PubR", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Parallel(f, g) =>
          edges ++= Seq(
            TransitionEdge(entry, "Par-Enter", EntryNode(astInScope(f))),
            TransitionEdge(entry, "Par-Enter", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "Par-PubL", exit),
            TransitionEdge(ExitNode(astInScope(g)), "Par-PubR", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case Future(f) =>
          edges ++= Seq(
            TransitionEdge(entry, "Future-Spawn", EntryNode(astInScope(f))),
            TransitionEdge(entry, "Future-Future", exit))
          edges ++= Seq(
            FutureEdge(ExitNode(astInScope(f)), exit))
          recurse(f)
        case Trim(f) =>
          edges ++= Seq(
            TransitionEdge(entry, "Trim-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Trim-Exit", exit))
          edges ++= Seq(
            ValueEdge(ExitNode(astInScope(f)), exit))
          recurse(f)
        case Force(xs, vs, b, f) =>
          xs foreach { declareVariable(entry, _) }
          vs foreach { touchArgument(_) }
          edges ++= Seq(
            TransitionEdge(entry, "Force-Enter", EntryNode(astInScope(f))),
            TransitionEdge(ExitNode(astInScope(f)), "Force-Exit", exit))
          edges ++= (xs zip vs) map { case (x, v) => ForceEdge(ValueNode(v), b, ValueNode(x)) }
          edges ++= vs.map(e => UseEdge(ValueNode(e), entry)) :+
            ValueEdge(ExitNode(astInScope(f)), exit)
          recurse(f)
        case IfDef(b, f, g) =>
          touchArgument(b)
          edges ++= Seq(
            TransitionEdge(entry, "IfDef-Def", EntryNode(astInScope(f))),
            TransitionEdge(entry, "IfDef-Not", EntryNode(astInScope(g))),
            TransitionEdge(ExitNode(astInScope(f)), "IfDef-L", exit),
            TransitionEdge(ExitNode(astInScope(g)), "IfDef-R", exit))
          edges ++= Seq(
            UseEdge(ValueNode(b), entry),
            ValueEdge(ExitNode(astInScope(f)), exit),
            ValueEdge(ExitNode(astInScope(g)), exit))
          recurse(f)
          recurse(g)
        case CallSite(target, args, _) =>
          touchArgument(target)
          args foreach touchArgument
          ValueNode(target).traceValue({
            case CallableNode(d@SpecificAST(_: Site, _)) => d
            case ValueNode(v@Constant(_)) => v
          }) match {
            case Some(s) =>
              //Logger.fine(s"Found that callsite calls one of: ${s.mkString("\n")}")
              if (s.forall(_.isInstanceOf[Site])) {
                for (d @ Site(_, _, _, _, _, _) <- s) {
                  connectCall(entry, exit, args, astInScope(d), true)
                }
              } else {
                for (d @ Site(_, _, _, _, _, _) <- s) {
                  connectCall(entry, exit, args, astInScope(d), false)
                }
                edges ++= Seq(AfterHaltEdge(entry, "CallSite", exit))
              }
            case None =>
              //Logger.fine(s"May call any site or anything else.")
              edges ++= Seq(AfterHaltEdge(entry, "CallSite", exit))
              onAllList += OnAll({ case CallableNode(d@SpecificAST(_: Site, _)) => d })(d => connectCall(entry, exit, args, d, false))
          }
          edges ++= args.map(e => UseEdge(ValueNode(e), entry)) :+
            UseEdge(ValueNode(target), entry)
        case CallDef(target, args, _) =>
          touchArgument(target)
          args foreach touchArgument
          ValueNode(target).traceValue({ case CallableNode(d@SpecificAST(_: Def, _)) => d }) match {
            case Some(s) =>
              //Logger.fine(s"Found that calldef calls one of: ${s.mkString("\n")}")
              for (d <- s) {
                connectCall(entry, exit, args, astInScope(d), true)
              }
            case None =>
              //Logger.fine(s"May call any def or anything else.")
              onAllList += OnAll({ case CallableNode(d@SpecificAST(_: Def, _)) => d })(d => connectCall(entry, exit, args, d, true))
          }
          edges ++= args.map(e => UseEdge(ValueNode(e), entry)) :+
            UseEdge(ValueNode(target), entry)
        case DeclareCallables(callables, body) =>
          callables.map(_.name) foreach { declareVariable(entry, _) }

          for (callable <- callables) {
            callable.formals foreach { declareVariable(entry, _) }
            val me = CallableNode(SpecificAST(callable, potentialPath))
            nodes += me
            edges += ValueEdge(me, ValueNode(callable.name))
            process(callable.body, callable :: potentialPath)
          }

          edges ++= Seq(
            TransitionEdge(entry, "Declare-Enter", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "Declare-Exit", exit))
          edges += ValueEdge(ExitNode(astInScope(body)), exit)
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
        case DeclareType(_, _, body) =>
          edges ++= Seq(
            TransitionEdge(entry, "", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "", exit))
          edges += ValueEdge(ExitNode(astInScope(body)), exit)
          recurse(body)
        case HasType(body, _) =>
          edges ++= Seq(
            TransitionEdge(entry, "", EntryNode(astInScope(body))),
            TransitionEdge(ExitNode(astInScope(body)), "", exit))
          edges += ValueEdge(ExitNode(astInScope(body)), exit)
          recurse(body)
      }
    }

    process(root, List())
    onAllList foreach { _() }

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
label="${quote(shortString(e))}";
${ns.map(n => s"""${idFor("n", n)} ${n.dotAttributeString};""").mkString("\n")}
}"""
      }.mkString("\n")
    }
    s"""
digraph {
$nodesStr
${edges.filter(_.isInstanceOf[ValueFlowEdge]).map(e => s"""${idFor("n", e.from)} -> ${idFor("n", e.to)} ${e.dotAttributeString};""").mkString("\n")}
}
"""
  }

  def debugShow(): Unit = {
    import java.nio.file.Files
    import java.nio.file.Paths
    import java.io.File
    import scala.sys.process._
    import java.io.ByteArrayInputStream
    import java.nio.charset.StandardCharsets
    val tmpDot = File.createTempFile("orcprog", ".gv");
    val outformat = "svg"
    val tmpPdf = File.createTempFile("orcprog", s".$outformat");
    //tmp.deleteOnExit();
    Logger.info(s"Wrote gz to $tmpDot")
    Logger.info(s"Wrote rendered to $tmpPdf")

    Files.write(Paths.get(tmpDot.toURI), toDot.getBytes(StandardCharsets.UTF_8))
    Seq("dot", s"-T$outformat", tmpDot.getAbsolutePath, s"-o${tmpPdf.getAbsolutePath}").!
    Seq("chromium-browser", tmpPdf.getAbsolutePath).!
  }
}

object FlowGraph {
  def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"
  def quote(s: String) = s.replace('"', '\'')

  trait DotAttributes {
    def dotAttributes: Map[String, String]

    def dotAttributeString = {
      s"[${dotAttributes.map(p => s"${p._1}=${'"'}${quote(p._2)}${'"'}").mkString(",")}]"
    }
  }

  case class SpecificAST[+T <: NamedAST](ast: T, path: List[NamedAST]) extends PrecomputeHashcode {
    (ast :: path).tails foreach {
      case b :: a :: _ =>
        assert(a.subtrees.toSet contains b, s"Path ${path.map(shortString).mkString("[", ", ", "]")} does not contain a parent of $ast.\n$b === is not a subtree of ===\n$a\n${a.subtrees}")
      case Seq(_) => true
      case Seq() => true
    }

    override def toString() = {
      s"$productPrefix($ast, ${path.map(shortString).mkString("[", ", ", "]")})"
    }
  }

  object SpecificAST {
    import scala.language.implicitConversions
    implicit def SpecificAST2AST[T <: NamedAST](l: SpecificAST[T]): T = l.ast
  }

  sealed abstract class Node extends DotAttributes with PrecomputeHashcode {
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

  trait WithSpecificAST extends Node {
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

  trait TokenFlowNode extends Node with WithSpecificAST {
    this: Product =>
    val location: SpecificAST[Expression]
  }

  trait ValueFlowNode extends Node {
    this: Product =>
    override def shape = "box"
    override def label = ast.toString()
    override def group: AnyRef = this
  }

  case class CallableNode(location: SpecificAST[Callable]) extends Node with ValueFlowNode with WithSpecificAST {
  }
  case class ValueNode(ast: Argument) extends Node with ValueFlowNode {
  }

  case class EntryNode(location: SpecificAST[Expression]) extends Node with TokenFlowNode {
    override def label = s"⤓ ${shortString(ast)}"
  }

  case class ExitNode(location: SpecificAST[Expression]) extends Node with TokenFlowNode {
    override def label = s"↧ ${shortString(ast)}"
  }

  sealed abstract class Edge extends DotAttributes with PrecomputeHashcode {
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
  trait HappensBeforeEdge extends Edge {
    this: Product =>

  }
  trait TokenFlowEdge extends HappensBeforeEdge {
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

  // Def/use chains
  trait DefUseEdge extends Edge {
    this: Product =>
    override def style: String = "dotted"
    override def color: String = "grey"
  }

  case class UseEdge(from: ValueNode, to: Node) extends Edge with DefUseEdge {
    override def label = "‣"
  }
  case class DefEdge(from: Node, to: ValueNode) extends Edge with DefUseEdge {
    override def label = "≜"
  }

  // Value flow edges
  trait ValueFlowEdge extends Edge {
    this: Product =>
    override def style: String = "dashed"
  }

  case class ValueEdge(from: Node, to: Node) extends Edge with ValueFlowEdge {
    override def label = ""
  }
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

}
