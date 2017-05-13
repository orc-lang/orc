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

class PublicationCountAnalysis(
  val publications: Map[SpecificAST[Expression], Option[Range]],
  results: Map[Node, PublicationCountAnalysis.PublicationInfo],
  graph: GraphDataProvider[Node, Edge])
  extends DebuggableGraphDataProvider[Node, Edge] {
  def edges = graph.edges
  def nodes = graph.nodes
  def subgraphs = Set()

  override def computedNodeDotAttributes(n: Node): DotAttributes = {
    results.get(n) match {
      case Some(PublicationCountAnalysis.PublicationInfo(p, f)) =>
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
      case (ExitNode(l), PublicationInfo(p, _)) => (l, p)
    }

    new PublicationCountAnalysis(r.toMap, res, cg)
  }

  def applyOrOverride[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
    (a, b) match {
      case (Some(a), Some(b)) => Some(f(a, b))
      case (None, Some(b)) => Some(b)
      case (Some(a), None) => Some(a)
      case (None, None) => None
    }
  def applyOrNone[T](a: Option[T], b: Option[T])(f: (T, T) => T): Option[T] =
    a.flatMap(a => b.map(b => f(a, b)))

  // TODO: Needs to convert any Range above a constant to infinity. That makes it a finite lattice.
  class PublicationInfo(val publications: Option[Range], val futureValues: Option[Range]) {
    assert(futureValues.forall(_ <= 1))

    def combine(o: PublicationInfo) =
      PublicationInfo(applyOrOverride(publications, o.publications)(_ + _), applyOrOverride(futureValues, o.futureValues)(_ union _))

    override def equals(o: Any) = o match {
      case PublicationInfo(p, f) if p == publications && f == futureValues =>
        true
      case _ =>
        false
    }

    override def hashCode() = publications.hashCode() + futureValues.hashCode()*37
    
    override def toString() = s"PublicationInfo($publications, $futureValues)"
  }
  object PublicationInfo {
    val maxPublications = 5

    def apply(publications: Option[Range], futureValues: Option[Range]) =
      new PublicationInfo(publications.map { r =>
        val rUpperBounded = if(r.maxi.exists(_ > maxPublications))
          Range(r.mini, None)
        else
          r
        if(rUpperBounded.mini > maxPublications)
          Range(maxPublications, rUpperBounded.maxi)
        else
          rUpperBounded
      }, futureValues)

    def unapply(info: PublicationInfo): Some[(Option[Range], Option[Range])] = Some((info.publications, info.futureValues))
  }

  class PublicationCountAnalyzer(graph: CallGraph) extends Analyzer {
    import graph._
    import FlowGraph._

    type NodeT = Node
    type StateT = PublicationInfo

    def initialNodes: collection.Set[Node] = {
      graph.nodesBy {
        case n @ (ValueNode(_) | VariableNode(_, _)) => n
      } + graph.entry
    }
    val initialState: PublicationInfo = PublicationInfo(None, None)

    def inputs(node: Node): collection.Set[Node] = {
      node.inEdges.map(_.from)
    }

    def outputs(node: Node): collection.Set[Node] = {
      node.outEdges.map(_.to)
    }

    def transfer(node: Node, old: PublicationInfo, messedup_inState: PublicationInfo, states: StateMap): (PublicationInfo, Set[Node]) = {
      // Compute the inState manually since it requires knowing the in edge types.
      def inStateOf[T <: Edge: ClassTag](n: Node) = {
        val ins = n.inEdgesOf[T].toSeq.map(_.from)
        val inVals = ins.map(n => states(n))
        val in = inVals.fold(initialState)(_ combine _)
        //Logger.fine(s"Processing $n in state of ${implicitly[ClassTag[T]].runtimeClass.getSimpleName}: ins = $ins; vals = $inVals\n    ===>  $in")
        in
      }
      def tokenOneOfOf[T <: Edge: ClassTag](n: Node) = {
        val ins = node.inEdgesOf[T].toSeq.map(_.from)
        val inVals = ins.map(n => Some(states(n).publications.getOrElse(Range(0, None))))
        val in = inVals.fold(None)(applyOrOverride(_, _)(_ union _))
        //Logger.fine(s"Processing $n tokenOneOf of ${implicitly[ClassTag[T]].runtimeClass.getSimpleName}: ins = $ins; vals = $inVals\n    ===>  $in")
        in
      }

      lazy val inStateValue = inStateOf[ValueEdge](node)
      lazy val inStateToken = inStateOf[TokenFlowEdge](node)
      lazy val inStateTokenOneOf = tokenOneOfOf[TokenFlowEdge](node)
      lazy val inStateUse = inStateOf[UseEdge](node)
      lazy val defaultFlowInState = PublicationInfo(inStateToken.publications, inStateValue.futureValues)

      val MaximumBoundedSet = CallGraph.BoundedSet.MaximumBoundedSet[CallGraph.FlowValue]

      val outState = node match {
        case EntryNode(SpecificAST(ast, Otherwise(l, r) :: _)) if ast == r =>
          // If we are on the right of an Otherwise then we need to transfer the pub count by the HappensBefore edge
          inStateOf[HappensBeforeEdge](node)
        case EntryNode(SpecificAST(ast, Callable(_, _, body, _, _, _) :: _)) if ast == body =>
          // If we are the body of a call. We need to union the publication inputs.
          PublicationInfo(inStateTokenOneOf, None)
        case EntryNode(SpecificAST(ast, _)) =>
          ast match {
            case n if node == graph.entry =>
              PublicationInfo(Some(Range(1, 1)), None)
            case Force(_, _, b, _) =>
              PublicationInfo(applyOrNone(inStateToken.publications, inStateUse.futureValues)(_ * _), inStateValue.futureValues)
            case _: BoundVar | Branch(_, _, _) | Parallel(_, _) | Future(_) | Constant(_) |
                 Call(_, _, _) | IfDef(_, _, _) | Trim(_) | DeclareCallables(_, _) | Otherwise(_, _) =>
              defaultFlowInState
          }
        case ExitNode(spAst@SpecificAST(ast, _)) =>
          ast match {
            case Future(_) =>
              PublicationInfo(inStateToken.publications, inStateValue.publications.map(_.limitTo(1)))
            case CallSite(target, _, _) => {
              lazy val entryInStateHappensBefore = inStateOf[HappensBeforeEdge](EntryNode(spAst))
              import CallGraph._
              val possibleV = graph.valuesOf[FlowValue](ValueNode(target, spAst.subtreePath))
              val extPubs = possibleV match {
                case BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[ExternalSiteValue]) =>
                  val pubss = s.toSeq.collect {
                    case ExternalSiteValue(site) =>
                      site.publications
                  }
                  entryInStateHappensBefore.publications.map(_ * pubss.reduce(_ union _))
                case _ =>
                  None
              }
              val intPubs = possibleV match {
                case BoundedSet.ConcreteBoundedSet(s) if s.exists(v => v.isInstanceOf[CallableValue]) =>
                  inStateTokenOneOf
                case _ =>
                  None
              }
              val otherPubs = possibleV match {
                case BoundedSet.ConcreteBoundedSet(s) if s.exists(v => !v.isInstanceOf[ExternalSiteValue] && !v.isInstanceOf[CallableValue]) =>
                  Some(Range(0, None))
                case MaximumBoundedSet =>
                  Some(Range(0, None))
                case _ =>
                  None
              }
              val p = applyOrOverride(extPubs, applyOrOverride(intPubs, otherPubs)(_ union _))(_ union _)
              PublicationInfo(p, Some(Range(1, 1)))
            }
            case CallDef(target, _, _) =>
              PublicationInfo(inStateTokenOneOf.orElse(Some(Range(0, None))), Some(Range(1, 1)))
            case IfDef(v, l, r) => {
              // This complicated mess is cutting around the graph. Ideally this information could be encoded in the graph, but this is flow sensitive?
              import CallGraph._
              val possibleV = graph.valuesOf[FlowValue](ValueNode(v, spAst.subtreePath))
              val isDef = possibleV match {
                case MaximumBoundedSet =>
                  None
                case BoundedSet.ConcreteBoundedSet(s) =>
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
              PublicationInfo(inStateToken.publications.map(_.limitTo(1)), inStateValue.futureValues)
            case Otherwise(l, r) =>
              val lState = states(ExitNode(SpecificAST(l, spAst.subtreePath)))
              val rState = states(ExitNode(SpecificAST(r, spAst.subtreePath)))
              if (lState.publications.exists(_ > 0)) {
                lState
              } else if (lState.publications.exists(_ only 0)) {
                rState
              } else {
                PublicationInfo(applyOrNone(lState.publications.map(_ intersect Range(1, None)), rState.publications)(_ union _), inStateValue.futureValues)
              }
            case _: BoundVar | Branch(_, _, _) | Force(_, _, _, _) | Parallel(_, _) | Constant(_) | DeclareCallables(_,_) =>
              defaultFlowInState
          }
        case VariableNode(x, ast) =>
          ast match {
            case Force(_, _, b, _) =>
              PublicationInfo(None, Some(Range(1, 1)))
            case DeclareCallables(_, _) | Callable(_, _, _, _, _, _) =>
              PublicationInfo(None, Some(Range(1, 1)))
            case Branch(_, _, _) =>
              defaultFlowInState
          }
        case ValueNode(_) | CallableNode(_, _) =>
          PublicationInfo(None, Some(Range(1, 1)))
        /*case FutureFieldNode(_, _) =>
          // FIXME: Probably not correct.
          PublicationInfo(Range(1, 1), inState.publications)
        case ArgumentFieldNode(_, _) =>
          inState*/
      }
      Logger.fine(s"Processing $node:  old=$old    out=$outState")

      (outState, Set())
    }

    def combine(a: PublicationInfo, b: PublicationInfo) = a combine b
  }

}
