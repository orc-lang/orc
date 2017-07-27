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
import scala.annotation.tailrec

import orc.ast.orctimizer.named._
import orc.compile.orctimizer.FlowGraph._
import scala.collection.mutable.Queue
import orc.compile.flowanalysis.Analyzer
import orc.compile.flowanalysis.BoundedSetModule
import orc.values.Field
import orc.values.sites.{ Site => ExtSite }
import orc.values.sites.{ InvokerMethod => ExtInvokerMethod }
import orc.compile.Logger
import orc.ast.PrecomputeHashcode
import scala.reflect.ClassTag
import orc.compile.flowanalysis.GraphDataProvider
import orc.util.DotUtils.shortString
import orc.compile.flowanalysis.DebuggableGraphDataProvider
import orc.util.DotUtils.DotAttributes
import orc.compile.AnalysisCache
import orc.compile.AnalysisRunner
import orc.compile.orctimizer.CallGraphValues.FutureValueSet
import orc.util.{ TTrue, TFalse, TUnknown }

/** Compute and store a call graph for the program stored in flowgraph.
  *
  */
class CallGraph(rootgraph: FlowGraph) extends DebuggableGraphDataProvider[Node, Edge] {
  import CallGraph._
  import CallGraphValues._

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

  def valuesOf(c: Node): FlowValueSet = {
    results.get(c).getOrElse(worstValueSet)
  }

  def valuesOf(e: BoundVar): FlowValueSet = {
    results.keys.find(n => n.ast == e && n.isInstanceOf[VariableNode]) match {
      case Some(n) =>
        valuesOf(n)
      case None =>
        worstValueSet
    }
  }

  def valuesOf(e: Expression.Z): FlowValueSet = {
    e match {
      case a: Argument.Z =>
        valuesOf(ValueNode(a))
      case e =>
        valuesOf(ExitNode(e))
    }
  }
  
  def targetsFromValue(targets: FlowValueSet): FlowValueSet = CallGraph.targetsFromValue(targets)
  
  @inline
  final def byCallTargetCases[A, B, C](target: Argument.Z)(externals: Set[AnyRef] => A, 
      internals: Set[Method.Z] => B, others: Set[CallGraphValues.Value[ObjectValueSet]] => C): (Option[A], Option[B], Option[C]) = {
    val possibleV = valuesOf(ValueNode(target))
    
    val extPubs = if(possibleV.exists({
      case n: NodeValue[_] => n.isExternalMethod.isTrue
      case _ => false
    })) {
      val vs = possibleV.toSet.collect {
        case n @ NodeValue(ConstantNode(Constant(site), _)) if n.isExternalMethod.isTrue => site
      }
      assert(vs.nonEmpty, s"Failed to get externals: $possibleV")
      Some(externals(vs))
    } else {
      None
    }
    
    val intPubs = if(possibleV.exists({
      case n: NodeValue[_] => n.isInternalMethod.isTrue
      case _ => false
    })) {
      val vs = possibleV.toSet.collect {
        case NodeValue(MethodNode(m, _)) => m
      }
      assert(vs.nonEmpty, s"Failed to get internals: $possibleV")
      Some(internals(vs))
    } else {
      None
    }
    
    val otherPubs = if(possibleV.exists({
      case n: NodeValue[_] if n.isMethod => false
      case _ => true
    })) {
      val vs = possibleV.toSet.flatMap({
        case n: NodeValue[_] if n.isMethod => None
        case v => Some(v)
      })
      assert(vs.nonEmpty, s"Failed to get others: $possibleV")
      Some(others(vs))
    } else {
      None
    }
    
    (extPubs, intPubs, otherPubs)
  }
  
  @inline
  final def byIfLenientCases[T](v: Argument.Z)(left: => T, right: => T, both: => T) = {
    val possibleV = valuesOf(ValueNode(v))

    val isDef = possibleV.view.map({
      case NodeValue(MethodNode(_: Routine.Z, _)) => TTrue
      case _ => TFalse
    }).fold(TUnknown)(_ union _)

    isDef match {
      case TTrue =>
        left
      case TFalse =>
        right
      case TUnknown =>
        both
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
          Map("tooltip" -> s"${n.label}\n${r.toSet.mkString(", ")}")
        case None => Map()
      }) ++
      (n match {
        case VariableNode(v, f: Method.Z) => Map("color" -> "darkgreen", "peripheries" -> "2")
        case _ => Map()
      })
  }
}

object CallGraph extends AnalysisRunner[(Expression.Z, Option[Method.Z]), CallGraph] {
  def compute(cache: AnalysisCache)(params: (Expression.Z, Option[Method.Z])): CallGraph = {
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
  
  import CallGraphValues._  

  type State = FlowValueSet
  
  val worstConcreteValueSet: ConcreteValueSet[ObjectValueSet] = ConcreteValueSet[ObjectValueSet](ObjectValueSet(), NodeValueSet[ObjectValueSet](EverywhereNode)) 
  val worstFutureValueSet: FutureValueSet[ObjectValueSet] = FutureValueSet[ObjectValueSet](worstConcreteValueSet, Set[Node](EverywhereNode)) 
  val worstValueSet: FlowValueSet = FlowValueSet(worstFutureValueSet, worstConcreteValueSet)
  val worstPublicationValueSet: FlowValueSet = FlowValueSet(FutureValueSet(), worstConcreteValueSet)

  type CallLocation = Call.Z

  case class AnalysisLocation[T <: NamedAST](stack: List[CallLocation], node: Node) {
    def limit(n: Int) = AnalysisLocation(stack.take(n), node)
  }

  def targetsFromValue(targets: FlowValueSet): FlowValueSet = {
    val potentialTargets = targets union targets.values.objects.flatMap({
      // Handle .apply calls
      case o: ObjectValue if o.get(Field("apply")).isDefined =>
        o.get(Field("apply")).get
      case v =>
        FlowValueSet()
    })
    
    potentialTargets
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
        case n @ MethodNode(_, _) => n
        case n @ ExitNode(New.Z(_, _, _, _)) if valueInputs(n).isEmpty => n
        case n @ ExitNode(Stop.Z()) => n
        case n @ ConstantNode(Constant(_), _) => n
      }).toSeq
    }
    val initialState: State = FlowValueSet()

    val additionalEdges = mutable.HashSet[ValueEdge]()
    val additionalEdgesNonValueFlow = mutable.HashSet[Edge]()

    def addEdge(e: ValueEdge): Boolean = {
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

    // TODO: PERFORMANCE: The filter operations are actually costing a notable amount of time. We may need to build an index or something.
    //          Should be able to implement a mutable subclass of GraphDataProvider and use it's indexing support and accessors.
    
    def valueInputs(node: Node): Seq[Edge] = {
      node.inEdgesOf[ValueEdge].toSeq ++ node.inEdgesOf[AfterEdge].toSeq ++ additionalEdges.filter(_.to == node)
    }

    def valueOutputs(node: Node): Seq[Edge] = {
      node.outEdgesOf[ValueEdge].toSeq ++ node.outEdgesOf[AfterEdge].toSeq ++ additionalEdges.filter(_.from == node)
    }

    def inputs(node: Node): collection.Seq[ConnectedNode] = {
      valueInputs(node).map(e => ConnectedNode(e, e.from))
    }

    def outputs(node: Node): collection.Seq[ConnectedNode] = {
      valueOutputs(node).map(e => ConnectedNode(e, e.to))
    }

    def transfer(node: Node, old: State, states: States): (State, Seq[Node]) = {
      // This is a def instead of a val (or lazy val) because it should be recomputed after nodes are added in the nodes computation.
      def inState = states.inStateReduced[ValueEdge](combine _)
      def entryState(n: ExitNode) = {
        states(EntryNode(n.location))
      }
      //def inStateWorst = states.inStateProcessed[ValueEdge, State](MaximumBoundedSet(), s => s, combine _)
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
              val potentialTargets = targetsFromValue(targets)
              
              // Select all callables with the correct arity.
              val methods = potentialTargets.filter({
                case c@NodeValue(MethodNode(m: Method.Z, _)) if m.formals.size == args.size =>
                  true
                case _ =>
                  false
                })
              
              if(potentialTargets.nonEmpty) {
                //Logger.finer(s"Targets: potentialTargets = $potentialTargets; callables = $callables")
              }
              
              // Build edges for arguments of this call site to all targets
              val argEdges = for {
                NodeValue(c: MethodNode) <- methods.view
                (formal, actual) <- (c.flowgraph.arguments zip args)
              } yield {
                //Logger.finer(s"Adding edges for argument ${formal.ast} from ${actual.value}")
                ValueEdge(ValueNode(actual), formal)
              }
              
              // Build edges for return value of this call site from all targets
              val retEdges = for {
                NodeValue(c: MethodNode) <- methods.view
              } yield {
                ValueEdge(c.flowgraph.exit, exit)
              }
              
              // The filter has a side effects. I feel appropriately bad about myself.
              val newEdges = (argEdges ++ retEdges).toSet filter addEdge
              
              // Add transitions into and out of the function body based on target results.
              for (NodeValue(c: MethodNode) <- methods.view) {
                //Logger.finer(s"Generating entry/exit edges for $c:\n${c.flowgraph.entry}\n${c.flowgraph.exit}")
                addEdge(TransitionEdge(entry, "Call-Inf", c.flowgraph.entry))
                addEdge(TransitionEdge(c.flowgraph.exit, "Return-Inf", exit))
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
          // ASSUMPTION: This assumes that there are no futures created outside Orc.
          val sources = states.inStateProcessed[ValueEdge, Set[Node]](
              Set(), 
              _.futures.valueSources, 
              _ ++ _)

          sources foreach { s =>
            addEdge(FutureValueSourceEdge(s, entry))
            //addEdge(FutureValueSourceEdge(s, exit))
          }
        case _ =>
          ()
      }

      // Compute the new state
      val state: State = node match {
        case n: ConstantNode =>
          NodeValue[ObjectValueSet](n).set
        case n: MethodNode =>          
          NodeValue[ObjectValueSet](n).set
        case VariableNode(v, f: Force.Z) =>
          FlowValueSet(FutureValueSet(), inState.futures.content ++ inState.values)
        case VariableNode(v, _) if valueInputs(node).nonEmpty =>
          inState
        case ExitNode(Future.Z(expr)) =>
          val inNode = ExitNode(expr)
          assert(inState.futures.isEmpty, s"Futures should never be inside futures\n$expr")
          FlowValueSet(FutureValueSet(inState.values, Set(inNode)), ConcreteValueSet())
        case ExitNode(GetMethod.Z(t)) =>
          val inNode = ValueNode(t)
          assert(inState.futures.isEmpty, s"Futures should never be passed to GetMethod\n$t")
          def isMethod(e: Value[_]) = e match {
            case n: NodeValue[_] if n.isMethod => true
            case _ => false
          }
          def isObjectWithApply(e: Value[_]) = e match {
            case o: ObjectValue if o.get(Field("apply")).isDefined => true
            case _ => false
          }
          val methods = inState.filter(isMethod(_))
          val callableObjects = inState.filter(isObjectWithApply(_)).flatMap({
            case o: ObjectValue if o.get(Field("apply")).isDefined => o.get(Field("apply")).get
            case _ => initialState
          })
          
          val nonMethods = inState.filter(v => !isMethod(v) && !isObjectWithApply(v))
          /*
          val nonMethodSet = if(nonMethods.nonEmpty) nonMethods ++ worstValueSet else initialState
          val nonMethodValues = nonMethodSet.values ++ nonMethodSet.futures.content
          */
          val nonMethodValues: ConcreteValueSet[ObjectValueSet] = if(nonMethods.nonEmpty) NodeValue[ObjectValueSet](node).set.values else ConcreteValueSet()
          val futureSource: Set[Node] = if(nonMethods.nonEmpty) Set(inNode) else Set()
          
          FlowValueSet(FutureValueSet(nonMethodValues, futureSource), nonMethodValues) ++ callableObjects ++ methods
        case ExitNode(GetField.Z(_, f)) =>
          val r = inState flatMap {
            case o: ObjectValue => o.get(f).getOrElse(FlowValueSet())
            case n: NodeValue[_] if n.isInternalMethod.isTrue => FlowValueSet()
            case _ => worstPublicationValueSet
          }
          r
        case node @ ExitNode(New.Z(self, _, bindings, _)) =>
          val structs = ObjectValue.buildStructures(node) { (field, inNode, refObject) =>
            field match {
              case f@FieldFuture(expr) =>
                val vs = states(inNode).view.map({
                  case f: FutureValue[_] =>
                    throw new AssertionError("Futures should never be inside futures")
                  case o@ObjectValue(nw, structs) =>
                    // This cannot be combined into the next case because refObject has side effects.
                    refObject(o).set
                  case e =>
                    e.set.toField
                })
                val v = vs.fold(FieldValueSet())(_ union _)
                
                FieldValueSet(FutureValueSet(v.values, Set(inNode)), ConcreteValueSet())
              case f@FieldArgument(a) =>
                val r = states(inNode).view.map({
                  case e: FutureValue[_] =>
                    // FIXME: This will cause errors if this contains any object which is converted.
                    e.set.toField
                  case o@ObjectValue(nw, structs) =>
                    // This cannot be combined into the next case because refObject has side effects.
                    refObject(o).set
                  case e =>
                    e.set.toField
                }).fold(FieldValueSet())(_ union _)
                
                r
            }
          }
          
          ObjectValue(node, structs).set
        case EntryNode(e @ Call.Z(target, args, _)) =>
          // Compute the possible values for external calls, this is combined with internal information in the ExitNode(Call) case.
          val exit = ExitNode(e)
          states.get(ValueNode(target)) match {
            case Some(targets) if targets.exists({
              case n: NodeValue[_] if !n.isExternalMethod.isFalse => true
              case n: NodeValue[_] if n.isMethod => false
              case _ => true
            }) =>
              NodeValue[ObjectValueSet](exit).set
            case _ =>
              initialState
          }
        case n @ ExitNode(Call.Z(target, args, _)) =>
          inState ++ entryState(n)
        case ExitNode(Stop.Z()) =>
          initialState
        case ExitNode(Force.Z(_, _, body)) =>
          states(ExitNode(body))
        case ExitNode(Resolve.Z(_, body)) =>
          states(ExitNode(body))
        case EntryNode(_: IfLenientMethod.Z) =>
          // Pass the check value through for use in the ExitNode
          inState
        case n @ ExitNode(IfLenientMethod.Z(v, l, r)) =>
          def isRoutine(e: Value[_]) = e match {
            case NodeValue(MethodNode(_: Routine.Z, _)) => true
            case _ => false
          }
          val routines = entryState(n).exists(isRoutine(_))
          val nonRoutines = entryState(n).exists(!isRoutine(_))
          
          if(routines && nonRoutines) {
            inState
          } else if(routines) {
            states(ExitNode(l))
          } else if(nonRoutines) {
            states(ExitNode(r))
          } else {
            assert(inState == initialState)
            initialState
          }
        case ExitNode(_) if valueInputs(node).nonEmpty =>
          // Pass through publications
          inState
        case EntryNode(_) =>
          // Ignore other entry nodes because they shouldn't matter
          initialState
      }

      ifDebugNode(node) {
        Logger.fine(s"Processed node: $node\n$state\n$nodes")
      }

      (state, nodes)
    }

    def combine(a: State, b: State) = {
      a union b
    }

    def moreCompleteOrEqual(a: State, b: State): Boolean = {
      b subsetOf a
    }
  }
}
