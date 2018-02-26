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

import orc.ast.orctimizer.named.{ Argument, BoundVar, Call, Constant, Expression, FieldArgument, FieldFuture, Force, Future, GetField, GetMethod, IfLenientMethod, Method, NamedAST, New, Resolve, Routine, Stop }
import orc.compile.{ AnalysisCache, AnalysisRunner, Logger }
import orc.compile.flowanalysis.{ Analyzer, DebuggableGraphDataProvider, GraphDataProvider, MutableGraphDataProvider }
import orc.compile.orctimizer.CallGraphValues.FutureValueSet
import orc.compile.orctimizer.FlowGraph.{ AfterEdge, ConstantNode, Edge, EntryNode, EverywhereNode, ExitNode, MethodNode, Node, ValueEdge, ValueNode, VariableNode, WithSpecificAST }
import orc.util.DotUtils.DotAttributes
import orc.values.Field

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
    (vrs.additionalEdges.edges ++ vrs.additionalEdgesNonValueFlow, r)
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
  
  object NodeInformation extends ValueBasedNodeInformation(this)
    
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

    val additionalEdges = new MutableGraphDataProvider[Node, Edge]()
    val additionalEdgesNonValueFlow = mutable.HashSet[Edge]()

    def addEdge(e: ValueEdge): Boolean = {
      if (additionalEdges.edges.contains(e) || edges.contains(e)) {
        false
      } else {
        // Logger.fine(s"Adding edge $e")
        additionalEdges.addEdge(e)
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

    def valueInputs(node: Node): Seq[Edge] = {
      node.inEdgesOf[ValueEdge].toSeq ++ node.inEdgesOf[AfterEdge].toSeq ++ additionalEdges.NodeAdds(node).inEdges.toSeq
    }

    def valueOutputs(node: Node): Seq[Edge] = {
      node.outEdgesOf[ValueEdge].toSeq ++ node.outEdgesOf[AfterEdge].toSeq ++ additionalEdges.NodeAdds(node).outEdges.toSeq
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
            case v @ NodeValue(ExitNode(Call.Z(Constant.Z(orc.lib.builtin.structured.TupleConstructor), _, _))) => v.set  
            case _ => initialState
          })
          
          val nonMethods = inState.filter(v => !isMethod(v) && !isObjectWithApply(v))
          val nonMethodValues: ConcreteValueSet[ObjectValueSet] = if(nonMethods.nonEmpty) NodeValue[ObjectValueSet](node).set.values else ConcreteValueSet()
          val futureSource: Set[Node] = if(nonMethods.nonEmpty) Set(inNode) else Set()
          
          FlowValueSet(FutureValueSet(nonMethodValues, futureSource), nonMethodValues) ++ callableObjects ++ methods
        case ExitNode(GetField.Z(_, f)) =>
          val r = inState flatMap {
            case o: ObjectValue => o.get(f).getOrElse(FlowValueSet())
            case n: NodeValue[_] if n.isInternalMethod.isTrue => FlowValueSet()
            case _ => worstValueSet
          }
          r
        case node @ ExitNode(New.Z(self, _, bindings, _)) =>
          val structs = ObjectValue.buildStructures(node) { (field, inNode) =>
            field match {
              case FieldFuture(expr) =>
                val (vs, oss) = states(inNode).view.map({
                  case f: FutureValue[_] =>
                    throw new AssertionError("Futures should never be inside futures")
                  case e =>
                    e.set.toField
                }).unzip
                val v = vs.fold(FieldValueSet())(_ union _)
                val os = oss.fold(ObjectStructureMap())(_ merge _)

                assert(v.futures.isEmpty)

                (FieldValueSet(FutureValueSet(v.values, Set(inNode)), ConcreteValueSet()), os)
              case FieldArgument(a) =>
                val (vs, oss) = states(inNode).view.map(_.set.toField).unzip
                val r = vs.fold(FieldValueSet())(_ union _)
                val os = oss.fold(ObjectStructureMap())(_ merge _)

                (r, os)
            }
          }

          ObjectValue(node, structs).set
        case EntryNode(e @ Call.Z(target, args, _)) =>
          // Compute the possible values for external calls, this is combined with internal information in the ExitNode(Call) case.
          val exit = ExitNode(e)
          states.get(ValueNode(target)) match {
            /*case Some(targets) if args.size == 2 && targets.existsForall({
              case n: NodeValue[_] if n.constantValue == Some(orc.lib.builtin.structured.TupleArityChecker) => true
              case _ => false
            }) => {
              // Pass through the first argument for TupleArityChecker
              states.get(ValueNode(args(0))).getOrElse(initialState)
            }*/
              
            case Some(targets) if targets.exists({
              case n: NodeValue[_] if !n.isExternalMethod.isFalse => true
              case n: NodeValue[_] if n.isMethod => false
              case _ => true
            }) => {
              NodeValue[ObjectValueSet](exit).set
            }
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
        case EntryNode(IfLenientMethod.Z(v, l, r)) =>
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
            // We may reach here if for whatever reason we are evaluated after l and r but before the entry node.
            // This can happen and is not invalid. We just return the initial state which will later be filled in.
            // The entry node will have to run eventually and will triger us to recompute.
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
