//
// PublicationCountAnalysis.scala -- Scala object and class PublicationCountAnalysis
// Project OrcScala
//
// Created by amp on May 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.compile.AnalysisRunner
import orc.compile.AnalysisCache
import orc.values.sites.Range
import orc.compile.flowanalysis._
import orc.ast.orctimizer.named._
import FlowGraph._
import orc.util.DotUtils.DotAttributes
import orc.compile.Logger
import scala.reflect.ClassTag
import orc.values.Field
import orc.ast.orctimizer.named.FieldAccess
import orc.compile.orctimizer.CallGraph.CallableValue

class PublicationCountAnalysis(
  results: Map[Node, PublicationCountAnalysis.PublicationInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  def edges = graph.edges
  def nodes = graph.nodes
  def subgraphs = Set()

  val expressions = results collect {
    case (ExitNode(l), p) => (l, p)
  }
  val values = results collect {
    case (n@ (_: ValueFlowNode), p) => (n, p)
  }

  def publicationsOf(e: SpecificAST[Expression]) =
    expressions.get(e).getOrElse(PublicationCountAnalysis.defaultResult).publications

  def stopabilityOf(e: ValueFlowNode) =
    values.get(e).getOrElse(PublicationCountAnalysis.defaultResult).futureValues

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    results.get(n) match {
      case Some(PublicationCountAnalysis.PublicationInfo(p, f, fs)) =>
        Map("label" -> s"${n.label}\n${p}p | v${f}")
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
object PublicationCountAnalysis extends AnalysisRunner[(Expression, Option[SpecificAST[Callable]]), PublicationCountAnalysis] {
  object BoundedSetInstance extends BoundedSetModule {
    type TU = ObjectInfo
    type TL = ObjectInfo
    val sizeLimit = 8

    class ConcreteBoundedSet[T >: TL <: TU](s: Set[T]) extends super.ConcreteBoundedSet[T](s) {
      override def union(o: BoundedSet[T]): BoundedSet[T] = o match {
        case ConcreteBoundedSet(s1) =>
          val ss = (s ++ s1)
          // Due to non-reified types I have casts here, but I sware it's safe.
          val objs = ss.collect({ case f: ObjectValue => f })
          val combinedObjs = objs.groupBy(_.root).map { case (_, os) =>
            os.reduce(_ ++ _)
          }
          BoundedSetInstance((ss -- objs.asInstanceOf[Set[T]]) ++ combinedObjs.asInstanceOf[Iterable[T]])
        case MaximumBoundedSet() =>
          MaximumBoundedSet()
      }
    }

    // For some reason this cannot be an object. It triggers an uninitialized field error.
    val ConcreteBoundedSet = new ConcreteBoundedSetCompanion {
      override def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    }
  }

  type BoundedSet[T >: ObjectInfo <: ObjectInfo] = BoundedSetInstance.BoundedSet[T]
  val BoundedSet: BoundedSetInstance.type = BoundedSetInstance
  type ConcreteBoundedSet[T >: ObjectInfo <: ObjectInfo] = BoundedSetInstance.ConcreteBoundedSet[T]
  val ConcreteBoundedSet = BoundedSetInstance.ConcreteBoundedSet
  type MaximumBoundedSet[T >: ObjectInfo <: ObjectInfo] = BoundedSetInstance.MaximumBoundedSet[T]
  val MaximumBoundedSet = BoundedSetInstance.MaximumBoundedSet()

  /*
   * The analysis is starts with the least specific bounds and reduces them until they cannot be reduced any more.
   * The initial bounds are 0-ω and are reduced toward singleton ranges. There is no empty range.
   */

  val defaultResult: PublicationInfo = PublicationInfo(Range(0, None), Range(0, 1), BoundedSet())

  def compute(cache: AnalysisCache)(params: (Expression, Option[SpecificAST[Callable]])): PublicationCountAnalysis = {
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

  object ObjectHandlingInstance extends ObjectHandling {
    type NodeT = Node
    type StoredValueT = PublicationInfo
    type ResultValueT = PublicationInfo

    val ObjectValueReified = implicitly[ClassTag[ObjectValue]]
    val ObjectRefReified = implicitly[ClassTag[ObjectRef]]

    case class ObjectRef(root: NodeT) extends ObjectRefBase
    object ObjectRef extends ObjectRefCompanion

    case class ObjectValue(root: NodeT, structures: Map[NodeT, ObjectStructure]) extends ObjectValueBase {
      def derefStoredValue(i: StoredValueT): ResultValueT = {
        val fields = i.fields map[ObjectInfo] derefObject
        i.copy(fields = fields)
      }

      def copy(root: NodeT, structs: Map[NodeT, ObjectStructure]): ObjectValue = {
        ObjectValue(root, structs)
      }
    }

    object ObjectValue extends ObjectValueCompanion
  }

  type ObjectInfo = ObjectHandlingInstance.ObjectInfo
  type ObjectValue = ObjectHandlingInstance.ObjectValue
  val ObjectValue = ObjectHandlingInstance.ObjectValue
  type ObjectRef = ObjectHandlingInstance.ObjectRef
  val ObjectRef = ObjectHandlingInstance.ObjectRef

  class PublicationInfo(val publications: Range, val futureValues: Range, val fields: BoundedSet[ObjectInfo]) extends LatticeValue[PublicationInfo] {
    assert(futureValues <= 1)

    def combine(o: PublicationInfo) = {
      PublicationInfo(
          publications intersect o.publications,
          futureValues intersect o.futureValues,
          fields ++ o.fields)
    }

    def copy(publications: Range = publications, futureValues: Range = futureValues, fields: BoundedSet[ObjectInfo] = fields) = {
      PublicationInfo(publications, futureValues, fields)
    }

    override def equals(o: Any) = o match {
      case PublicationInfo(p, f, fields) if p == publications && f == futureValues && fields == this.fields =>
        true
      case _ =>
        false
    }

    override def hashCode() = publications.hashCode() + (futureValues.hashCode() + fields.hashCode() * 37) * 37

    override def toString() = s"PublicationInfo($publications, $futureValues, $fields)"

    def lessThan(o: PublicationInfo) = o moreCompleteOrEqual this

    def moreCompleteOrEqual(o: PublicationInfo): Boolean = {
      val fieldsMoreComplete = {
        (o.fields, fields) match {
          case (ConcreteBoundedSet(so), ConcreteBoundedSet(st)) =>
            so forall { vo =>
              st exists { vt =>
                vo subsetOf vt
              }
            }
          case (ConcreteBoundedSet(so), MaximumBoundedSet) =>
            true
          case (MaximumBoundedSet, ConcreteBoundedSet(st)) =>
            false
          case (MaximumBoundedSet, MaximumBoundedSet) =>
            true
        }
      }

      (publications subsetOf o.publications) &&
      (futureValues subsetOf o.futureValues) &&
      fieldsMoreComplete
    }
  }

  object PublicationInfo {
    val maxPublications = 2

    def apply(publications: Range, futureValues: Range, fields: BoundedSet[ObjectInfo]) =
      new PublicationInfo({
        val rUpperBounded = if (publications.maxi.exists(_ > maxPublications))
          Range(publications.mini, None)
        else
          publications
        if (rUpperBounded.mini > maxPublications)
          Range(maxPublications, rUpperBounded.maxi)
        else
          rUpperBounded
      }, futureValues, fields)

    def unapply(info: PublicationInfo): Some[(Range, Range, BoundedSet[ObjectInfo])] =
      Some((info.publications, info.futureValues, info.fields))
  }

  class PublicationCountAnalyzer(graph: CallGraph) extends Analyzer {
    import graph._
    import FlowGraph._

    type NodeT = Node
    type EdgeT = Edge
    type StateT = PublicationInfo

    def initialNodes: collection.Seq[Node] = {
      (graph.nodesBy {
        case n @ (ValueNode(_) | VariableNode(_, _)) => n
        case n @ ExitNode(SpecificAST(Stop(), _)) => n
      }).toSeq :+ graph.entry
    }
    val initialState: PublicationInfo = PublicationInfo(Range(0, None), Range(0, 1), BoundedSet())

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      node.inEdges.map(e => ConnectedNode(e, e.from)).toSeq ++ {
        node match {
          case ExitNode(b@SpecificAST(Branch(l, _, _), _)) =>
            val n = ExitNode(SpecificAST(l, b.subtreePath))
            Set(ConnectedNode(UseEdge(n, node), n))
          case _ =>
            Set()
        }
      }
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      node.outEdges.map(e => ConnectedNode(e, e.to)).toSeq ++ {
        node match {
          case ExitNode(spAst @ SpecificAST(ast, Branch(l, _, _) :: _)) if ast == l =>
            val n = ExitNode(SpecificAST(spAst.path.head.asInstanceOf[Branch], spAst.path.tail))
            Set(ConnectedNode(UseEdge(node, n), n))
          case _ =>
            Set()
        }
      }
    }

    def transfer(node: Node, old: PublicationInfo, states: States): (PublicationInfo, Seq[Node]) = {
      // TODO: Check and make sure all the exit counts are the number of exits from that node if one entered as designed.

      //Logger.fine(s"Processing $node: inputs:\n${inputs(node).map(cn => s"$cn -> ${states(cn.node)}").mkString("\n")}")

      def mergeInputs(a: PublicationInfo, b: PublicationInfo) =
        PublicationInfo(a.publications + b.publications, a.futureValues union b.futureValues, a.fields union b.fields)

      lazy val inStateValue = states.inStateReduced[ValueEdge](mergeInputs)
      lazy val inStateTokenOneOf = states.inStateProcessed[TokenFlowEdge, Range](
        Range(0, None), _.publications, _ union _)
      lazy val inStateTokenAllOf = states.inStateProcessed[TokenFlowEdge, Range](
        Range(0, None), _.publications, _ + _)

      lazy val inStateFlow = node match {
        case EntryNode(SpecificAST(ast, Otherwise(l, r) :: _)) if ast == r =>
          // If we are on the right of an Otherwise then we need to transfer the pub count by the HappensBefore edge
          states.inStateReduced[HappensBeforeEdge](mergeInputs)
        case EntryNode(SpecificAST(ast, Callable(_, _, body, _, _, _) :: _)) if ast == body =>
          // If we are the body of a call. We need to union the publication inputs.
          PublicationInfo(inStateTokenOneOf, Range(0, 1), BoundedSet())
        case _ =>
          states.inStateReduced[TokenFlowEdge](mergeInputs)
      }

      lazy val inStateUse = states.inStateReduced[UseEdge](mergeInputs)
      lazy val defaultFlowInState = PublicationInfo(inStateFlow.publications, inStateValue.futureValues, inStateValue.fields)

      lazy val nonFutureVariableState = PublicationInfo(Range(0, None), Range(1, 1), inStateValue.fields)

      //Logger.fine(s"Processing $node: inState: value = $inStateValue, flow = $inStateFlow") //  use = $inStateUse

      val MaximumBoundedSet = CallGraph.BoundedSet.MaximumBoundedSet[CallGraph.FlowValue]

      val outState = node match {
        case EntryNode(SpecificAST(ast, _)) =>
          ast match {
            case n if node == graph.entry =>
              PublicationInfo(Range(1, 1), Range(0, 1), BoundedSet())
            case Force(_, _, b, _) =>
              PublicationInfo(inStateUse.futureValues, Range(0, 1), BoundedSet())
            case _: BoundVar | Branch(_, _, _) | Parallel(_, _) | Future(_) | Constant(_) |
              Call(_, _, _) | IfDef(_, _, _) | Trim(_) | DeclareCallables(_, _) | Otherwise(_, _) |
              New(_, _, _, _) | FieldAccess(_, _) | DeclareType(_, _, _) | HasType(_, _) | Stop() =>
              PublicationInfo(Range(1, 1), Range(0, 1), BoundedSet())
          }
        case node@ExitNode(spAst @ SpecificAST(ast, _)) =>
          ast match {
            case Future(_) =>
              PublicationInfo(inStateFlow.publications, inStateValue.publications.limitTo(1), inStateValue.fields)
            case CallSite(target, _, _) => {
              lazy val inStateHappensBefore = states.inStateReduced[HappensBeforeEdge](mergeInputs)
              import CallGraph.{FlowValue, ExternalSiteValue, CallableValue}
              val possibleV = graph.valuesOf[FlowValue](ValueNode(target, spAst.subtreePath))
              val extPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[ExternalSiteValue]) =>
                  val pubss = s.toSeq.collect {
                    case ExternalSiteValue(site) =>
                      site.publications
                  }
                  Some(inStateHappensBefore.publications * pubss.reduce(_ union _))
                case _ =>
                  None
              }
              val intPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[CallableValue]) =>
                  Some(inStateTokenOneOf)
                case _ =>
                  None
              }
              val otherPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => !v.isInstanceOf[ExternalSiteValue] && !v.isInstanceOf[CallableValue]) =>
                  Some(Range(0, None))
                case MaximumBoundedSet =>
                  Some(Range(0, None))
                case _ =>
                  None
              }
              val p = applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ union _))(_ union _).getOrElse(Range(0, None))
              PublicationInfo(p, Range(1, 1), inStateValue.fields)
            }
            case CallDef(target, _, _) =>
              PublicationInfo(inStateTokenOneOf, Range(1, 1), inStateValue.fields)
            case IfDef(v, l, r) => {
              // This complicated mess is cutting around the graph. Ideally this information could be encoded in the graph, but this is flow sensitive?
              import CallGraph.{FlowValue, ExternalSiteValue, CallableValue}
              val possibleV = graph.valuesOf[CallGraph.FlowValue](ValueNode(v, spAst.subtreePath))
              val isDef = possibleV match {
                case MaximumBoundedSet =>
                  None
                case CallGraph.ConcreteBoundedSet(s) =>
                  val (ds, nds) = s.partition {
                    case CallableValue(callable: Def, _) =>
                      true
                    case _ =>
                      false
                  }
                  (ds.nonEmpty, nds.nonEmpty) match {
                    case (true, false) =>
                      Some(true)
                    case (false, true) =>
                      Some(false)
                    case _ =>
                      None
                  }
              }
              val realizableIn = isDef match {
                case Some(true) =>
                  states(ExitNode(SpecificAST(l, spAst.subtreePath)))
                case Some(false) =>
                  states(ExitNode(SpecificAST(r, spAst.subtreePath)))
                case None =>
                  defaultFlowInState
              }
              realizableIn
            }
            case Trim(_) =>
              PublicationInfo(inStateFlow.publications.limitTo(1), inStateValue.futureValues, inStateValue.fields)
            case New(_, _, bindings, _) =>
              val obj = ObjectValue.buildStructures(node) { (content, inNode, refObject) =>
                val st = states(inNode)
                val fields = st.fields map refObject
                content match {
                  case f @ FieldFuture(expr) =>
                    PublicationInfo(Range(1, 1), st.publications.limitTo(1), fields)
                  case f @ FieldArgument(a) =>
                    PublicationInfo(Range(1, 1), st.futureValues, fields)
                }
              }
              PublicationInfo(inStateFlow.publications, Range(1, 1), BoundedSet(obj))
            case FieldAccess(_, f) =>
              inStateValue.fields.values match {
                case Some(s) =>
                  s.map(_.get(f)).fold(None)(applyOrOverride(_, _)(_ combine _)).getOrElse(initialState)
                case None =>
                  PublicationInfo(Range(0, None), Range(0, None), PublicationCountAnalysis.BoundedSet.MaximumBoundedSet())
              }
            case Otherwise(l, r) =>
              val lState = states(ExitNode(SpecificAST(l, spAst.subtreePath)))
              val rState = states(ExitNode(SpecificAST(r, spAst.subtreePath)))
              if (lState.publications > 0) {
                lState
              } else if (lState.publications only 0) {
                rState
              } else {
                PublicationInfo((lState.publications intersect Range(1, None)) union rState.publications, inStateValue.futureValues, inStateValue.fields)
              }
            case Stop() =>
              PublicationInfo(Range(0, 0), Range(0, 0), BoundedSet())
            case Force(_, _, b, _) =>
              PublicationInfo(inStateFlow.publications * inStateUse.futureValues, inStateValue.futureValues, inStateValue.fields)
            case Branch(_, _, _) =>
              PublicationInfo(inStateFlow.publications * inStateUse.publications, inStateValue.futureValues, inStateValue.fields)
            case _: BoundVar | Parallel(_, _) | Constant(_) |
                DeclareCallables(_, _) | DeclareType(_, _, _) | HasType(_, _) =>
              defaultFlowInState
          }
        case VariableNode(x, ast) =>
          ast match {
            case Force(_, _, b, _) =>
              nonFutureVariableState
            case New(_, _, _, _) =>
              nonFutureVariableState
            case DeclareCallables(_, _) | Callable(_, _, _, _, _, _) =>
              nonFutureVariableState
            case Branch(_, _, _) =>
              defaultFlowInState
          }
        case ValueNode(_) | CallableNode(_, _) =>
          nonFutureVariableState
      }
      //Logger.fine(s"Processed $node:  old=$old    out=$outState")

      (outState, Nil)
    }

    def combine(a: PublicationInfo, b: PublicationInfo) = {
      a combine b
    }

    def moreCompleteOrEqual(a: PublicationInfo, b: PublicationInfo): Boolean = {
      a moreCompleteOrEqual b
    }
  }

}
