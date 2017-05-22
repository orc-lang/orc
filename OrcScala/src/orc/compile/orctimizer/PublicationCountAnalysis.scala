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

class PublicationCountAnalysis(
  val publications: Map[SpecificAST[Expression], Range],
  results: Map[Node, PublicationCountAnalysis.PublicationInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  def edges = graph.edges
  def nodes = graph.nodes
  def subgraphs = Set()

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    results.get(n) match {
      case Some(PublicationCountAnalysis.PublicationInfo(p, f, fs)) =>
        Map("label" -> s"${n.label}\n${p.getOrElse("")}p | v${f.getOrElse("")}")
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
  def compute(cache: AnalysisCache)(params: (Expression, Option[SpecificAST[Callable]])): PublicationCountAnalysis = {
    val cg = cache.get(CallGraph)(params)
    val a = new PublicationCountAnalyzer(cg)
    val res = a()
    val r = res collect {
      case (ExitNode(l), PublicationInfo(Some(p), _, _)) => (l, p)
    }

    new PublicationCountAnalysis(r.toSeq.toMap, res, cg)
  }

  val BoundedSet: BoundedSetModule {
    type TU = ObjectInfo
    type TL = Nothing
  } = new BoundedSetModule {
    mod =>
    type TU = ObjectInfo
    type TL = Nothing
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
          mod((ss -- objs.asInstanceOf[Set[T]]) ++ combinedObjs.asInstanceOf[Iterable[T]])
        case MaximumBoundedSet() =>
          MaximumBoundedSet()
      }
    }

    override object ConcreteBoundedSet extends ConcreteBoundedSetObject {
      override def apply[T >: TL <: TU](s: Set[T]) = new ConcreteBoundedSet(s)
    }
  }

  import BoundedSet._

  def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
    (a, b) match {
      case (Some(a), Some(b)) => Some(f(a, b))
      case (None, Some(b)) => Some(b)
      case (Some(a), None) => Some(a)
      case (None, None) => None
    }
  def applyOrNone[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] = {
    a.flatMap(a => b.map(b => f(a, b)))
  }

  sealed abstract class ObjectInfo {
    val root: Node
    def apply(f: Field): Option[PublicationInfo]
    def ++(o: ObjectValue): ObjectValue
    def ++(o: ObjectInfo): ObjectValue = o match {
      case o: ObjectValue => this ++ o
    }
    def subsetOf(o: ObjectValue): Boolean
    def subsetOf(o: ObjectInfo): Boolean = o match {
      case o: ObjectValue => this subsetOf o
      case o if root == o.root => true
      case _ => false
    }
  }

  case class ObjectRef(root: Node) extends ObjectInfo {
    def apply(f: Field): Option[PublicationInfo] = throw new AssertionError("Should never be called")
    def ++(o: ObjectValue): ObjectValue = throw new AssertionError("Should never be called")
    def subsetOf(o: ObjectValue): Boolean = root == o.root
  }

  case class ObjectValue(root: Node, structures: Map[Node, Map[Field, PublicationInfo]]) extends ObjectInfo with ObjectHandling {
    type NodeT = Node
    type StoredValueT = PublicationInfo
    type This = ObjectValue

    override def toString() = s"ObjectValue($root, ${structures(root)})"

    def apply(f: Field): Option[PublicationInfo] = {
      def flatten(i: PublicationInfo): PublicationInfo = {
        val fields = i.fields map[ObjectInfo] {
          case ObjectRef(i) =>
            lookupObject(i)
          case v =>
            v
        }
        i.copy(fields = fields)
      }

      get(f) map { flatten }
    }

    /*def combineStored(a: PublicationInfo, b: PublicationInfo): PublicationInfo = {
      a combine b
    }*/

    def copyObject(root: FlowGraph.Node, structs: Map[FlowGraph.Node, Map[Field, PublicationInfo]]): ObjectValue = {
      ObjectValue(root, structs)
    }

    /*def subsetOfStored(a: PublicationInfo, b: PublicationInfo): Boolean = {
      b moreCompleteOrEqual a
    }*/
  }

  object ObjectValue extends ObjectHandlingCompanion {
    type NodeT = Node
    type StoredValueT = PublicationInfo
    type Instance = ObjectValue
  }

  class PublicationInfo(val publications: Option[Range], val futureValues: Option[Range], val fields: BoundedSet[ObjectInfo]) extends LatticeValue[PublicationInfo] {
    assert(futureValues.forall(_ <= 1))

    def combine(o: PublicationInfo) = {
      PublicationInfo(
          applyOrOverride(publications, o.publications)(_ + _),
          applyOrOverride(futureValues, o.futureValues)(_ union _),
          fields ++ o.fields)
    }

    def copy(publications: Option[Range] = publications, futureValues: Option[Range] = futureValues, fields: BoundedSet[ObjectInfo] = fields) = {
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

    private def optRangeLessThanOrEqual(a: Option[Range], b: Option[Range]): Boolean = {
      (a, b) match {
        case (_, None) => true
        case (Some(r), Some(l)) => r supersetOf l
        case _ => false
      }
    }
    
    def lessThan(o: PublicationInfo) = o moreCompleteOrEqual this

    def moreCompleteOrEqual(o: PublicationInfo): Boolean = {
      val fieldsMoreComplete = {
        val MaximumBoundedSet = BoundedSet.MaximumBoundedSet[ObjectInfo]()
        (o.fields, fields) match {
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

      (optRangeLessThanOrEqual(publications, o.publications) ||
          ((publications, o.publications) match {
            case (_, None) => true
            case (Some(Range(n, _)), Some(Range(_, Some(m)))) => n > m
            case _ => false
          })) &&
      optRangeLessThanOrEqual(futureValues, o.futureValues) &&
      fieldsMoreComplete
    }
  }
  object PublicationInfo {
    val maxPublications = 5

    def apply(publications: Option[Range], futureValues: Option[Range], fields: BoundedSet[ObjectInfo]) =
      new PublicationInfo(publications.map { r =>
        val rUpperBounded = if (r.maxi.exists(_ > maxPublications))
          Range(r.mini, None)
        else
          r
        if (rUpperBounded.mini > maxPublications)
          Range(maxPublications, rUpperBounded.maxi)
        else
          rUpperBounded
      }, futureValues, fields)

    def unapply(info: PublicationInfo): Some[(Option[Range], Option[Range], BoundedSet[ObjectInfo])] = Some((info.publications, info.futureValues, info.fields))
  }

  class PublicationCountAnalyzer(graph: CallGraph) extends Analyzer {
    import graph._
    import FlowGraph._

    type NodeT = Node
    type EdgeT = Edge
    type StateT = PublicationInfo

    def initialNodes: collection.Set[Node] = {
      graph.nodesBy {
        case n @ (ValueNode(_) | VariableNode(_, _)) => n
        case n @ ExitNode(SpecificAST(Stop(), _)) => n
      } + graph.entry
    }
    val initialState: PublicationInfo = PublicationInfo(None, None, BoundedSet())

    def inputs(node: Node): collection.Set[ConnectedNode] = {
      node.inEdges.map(e => ConnectedNode(e, e.from)) ++ {
        node match {
          case ExitNode(b@SpecificAST(Branch(l, _, _), _)) =>
            val n = ExitNode(SpecificAST(l, b.subtreePath))
            Set(ConnectedNode(UseEdge(n, node), n))
          case _ =>
            Set()
        }
      }
    }

    def outputs(node: Node): collection.Set[ConnectedNode] = {
      node.outEdges.map(e => ConnectedNode(e, e.to)) ++ {
        node match {
          case ExitNode(spAst @ SpecificAST(ast, Branch(l, _, _) :: _)) if ast == l =>
            val n = ExitNode(SpecificAST(spAst.path.head.asInstanceOf[Branch], spAst.path.tail))
            Set(ConnectedNode(UseEdge(node, n), n))
          case _ =>
            Set()
        }
      }
    }

    def transfer(node: Node, old: PublicationInfo, states: States): (PublicationInfo, Set[Node]) = {
      // FIXME: This does not handle recursion. In fact it might actually produce bogus results instead of just imprecise results.

      //Logger.fine(s"Processing $node: inputs:\n${inputs(node).map(cn => s"$cn -> ${states(cn.node)}").mkString("\n")}")

      lazy val inStateValue = states.inState[ValueEdge]()
      lazy val inStateTokenOneOf = states.inStateProcessed[TokenFlowEdge, Option[Range]](
        None, _.publications,
        applyOrNone(_, _)(_ union _))

      lazy val inStateFlow = node match {
        case EntryNode(SpecificAST(ast, Otherwise(l, r) :: _)) if ast == r =>
          // If we are on the right of an Otherwise then we need to transfer the pub count by the HappensBefore edge
          states.inState[HappensBeforeEdge]()
        case EntryNode(SpecificAST(ast, Callable(_, _, body, _, _, _) :: _)) if ast == body =>
          // If we are the body of a call. We need to union the publication inputs.
          PublicationInfo(inStateTokenOneOf, None, BoundedSet())
        case _ =>
          states.inState[TokenFlowEdge]()
      }

      lazy val inStateUse = states.inState[UseEdge]()
      lazy val defaultFlowInState = PublicationInfo(inStateFlow.publications, inStateValue.futureValues, inStateValue.fields)

      //Logger.fine(s"Processing $node: inState: value = $inStateValue, flow = $inStateFlow") //  use = $inStateUse

      val MaximumBoundedSet = CallGraph.BoundedSet.MaximumBoundedSet[CallGraph.FlowValue]

      val outState = node match {
        case EntryNode(SpecificAST(ast, _)) =>
          ast match {
            case n if node == graph.entry =>
              PublicationInfo(Some(Range(1, 1)), None, BoundedSet())
            case Force(_, _, b, _) =>
              PublicationInfo(inStateUse.futureValues, None, BoundedSet())
            case _: BoundVar | Branch(_, _, _) | Parallel(_, _) | Future(_) | Constant(_) |
              Call(_, _, _) | IfDef(_, _, _) | Trim(_) | DeclareCallables(_, _) | Otherwise(_, _) |
              New(_, _, _, _) | FieldAccess(_, _) | DeclareType(_, _, _) | HasType(_, _) | Stop() =>
              PublicationInfo(Some(Range(1, 1)), None, BoundedSet())
          }
        case node@ExitNode(spAst @ SpecificAST(ast, _)) =>
          ast match {
            case Future(_) =>
              PublicationInfo(inStateFlow.publications, inStateValue.publications.map(_.limitTo(1)), inStateValue.fields)
            case CallSite(target, _, _) => {
              lazy val inStateHappensBefore = states.inState[HappensBeforeEdge]()
              import CallGraph._
              val possibleV = graph.valuesOf[FlowValue](ValueNode(target, spAst.subtreePath))
              val extPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[ExternalSiteValue]) =>
                  val pubss = s.toSeq.collect {
                    case ExternalSiteValue(site) =>
                      site.publications
                  }
                  inStateHappensBefore.publications.map(_ * pubss.reduce(_ union _))
                case _ =>
                  None
              }
              val intPubs = possibleV match {
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[CallableValue]) =>
                  inStateTokenOneOf
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
              val p = applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ union _))(_ union _)
              PublicationInfo(p, Some(Range(1, 1)), inStateValue.fields)
            }
            case CallDef(target, _, _) =>
              PublicationInfo(inStateTokenOneOf, Some(Range(1, 1)), inStateValue.fields)
            case IfDef(v, l, r) => {
              // This complicated mess is cutting around the graph. Ideally this information could be encoded in the graph, but this is flow sensitive?
              import CallGraph._
              val possibleV = graph.valuesOf[FlowValue](ValueNode(v, spAst.subtreePath))
              val isDef = possibleV match {
                case MaximumBoundedSet =>
                  None
                case CallGraph.BoundedSet.ConcreteBoundedSet(s) =>
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
              PublicationInfo(inStateFlow.publications.map(_.limitTo(1)), inStateValue.futureValues, inStateValue.fields)
            case New(_, _, bindings, _) =>
              val structs = ObjectValue.buildStructures(node) { (content, inNode, addObject) =>
                val st = states(inNode)
                val fields = st.fields map {
                  case o @ ObjectValue(root, _) =>
                    addObject(o)
                    ObjectRef(root)
                  case v => v
                }
                content match {
                  case f @ FieldFuture(expr) =>
                    PublicationInfo(Some(Range(1, 1)), st.publications.map(_.limitTo(1)), fields)
                  case f @ FieldArgument(a) =>
                    PublicationInfo(Some(Range(1, 1)), st.futureValues, fields)
                }
              }
              PublicationInfo(inStateFlow.publications, Some(Range(1, 1)), BoundedSet(ObjectValue(node, structs)))
            case FieldAccess(_, f) =>
              inStateValue.fields.values match {
                case Some(s) =>
                  s.map(_(f)).fold(None)(applyOrOverride(_, _)(_ combine _)).getOrElse(initialState)
                case None =>
                  PublicationInfo(Some(Range(0, None)), Some(Range(0, None)), PublicationCountAnalysis.BoundedSet.MaximumBoundedSet())
              }
            case Otherwise(l, r) =>
              val lState = states(ExitNode(SpecificAST(l, spAst.subtreePath)))
              val rState = states(ExitNode(SpecificAST(r, spAst.subtreePath)))
              // TODO: Unioning in old basically just forces monotonicity at the cost of precision. Once the TODO below is fixed this should go away too.
              PublicationInfo(applyOrOverride(applyOrOverride(lState.publications, rState.publications)(_ union _), old.publications)(_ union _), inStateValue.futureValues, inStateValue.fields)
              // TODO: Ideally this would handle useful special cases (as below). However because changes need to be monotonic they cannot since it could trigger jumping from lstate to rstate.
              //    Part of the issue is that I am allowing the number of publications to increase in some cases not just the range to widen. 
              //    I think I need to figure out how to delay range computation a bit so that it will never need increase the number of publications only widen the range. I have no idea how to make that work with cycles though.
              /*
              if (lState.publications.exists(_ > 0)) {
                lState
              } else if (lState.publications.exists(_ only 0)) {
                rState
              } else if (lState.publications.isDefined) {
                PublicationInfo(applyOrOverride(lState.publications.map(_ intersect Range(1, None)), rState.publications)(_ union _), inStateValue.futureValues, inStateValue.fields)
              } else {
                initialState
              }
              */
            case Stop() =>
              PublicationInfo(Some(Range(0, 0)), None, BoundedSet())
            case Force(_, _, b, _) =>
              PublicationInfo(applyOrNone(inStateFlow.publications, inStateUse.futureValues)(_ * _), inStateValue.futureValues, inStateValue.fields)
            case Branch(_, _, _) =>
              // TODO: Check and make sure all the exit counts are the number of exits from that node if one entered as designed.
              PublicationInfo(applyOrNone(inStateFlow.publications, inStateUse.publications)(_ * _), inStateValue.futureValues, inStateValue.fields)
            case _: BoundVar | Parallel(_, _) | Constant(_) |
                DeclareCallables(_, _) | DeclareType(_, _, _) | HasType(_, _) =>
              defaultFlowInState
          }
        case VariableNode(x, ast) =>
          ast match {
            case Force(_, _, b, _) =>
              PublicationInfo(None, Some(Range(1, 1)), inStateValue.fields)
            case New(_, _, _, _) =>
              PublicationInfo(None, Some(Range(1, 1)), inStateValue.fields)
            case DeclareCallables(_, _) | Callable(_, _, _, _, _, _) =>
              PublicationInfo(None, Some(Range(1, 1)), inStateValue.fields)
            case Branch(_, _, _) =>
              defaultFlowInState
          }
        case ValueNode(_) | CallableNode(_, _) =>
          PublicationInfo(None, Some(Range(1, 1)), inStateValue.fields)
      }
      //Logger.fine(s"Processed $node:  old=$old    out=$outState")

      (outState, Set())
    }

    def combine(a: PublicationInfo, b: PublicationInfo) = {
      a combine b
    }

    def moreCompleteOrEqual(a: PublicationInfo, b: PublicationInfo): Boolean = {
      a moreCompleteOrEqual b
    }
  }

}
