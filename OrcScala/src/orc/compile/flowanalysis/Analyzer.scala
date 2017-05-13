package orc.compile.flowanalysis

import scala.collection.immutable.Queue
import scala.annotation.tailrec
import scala.collection.immutable.HashMap
import collection.Set

//trait Node[T <: Node[T]] {
//  def outputs: Set[T]
//  def inputs: Set[T]
//}

//trait AnalysisValue[T <: AnalysisValue[T]] {
//  def combine(v: T): T
//}

abstract class Analyzer {
  type NodeT
  type StateT
  type StateMap = Map[NodeT, StateT]

  def initialNodes: Set[NodeT]
  def initialState: StateT

  def apply(): StateMap = {
    @tailrec
    def process(work: Queue[NodeT], states: StateMap): StateMap = {
      work.dequeueOption match {
        case Some((node, rest)) => {
          val inState = inputs(node).map(states.getOrElse(_, initialState)).fold(initialState)(combine)
          val oldState = states.getOrElse(node, initialState)
          val (newState, newNodes) = transfer(node, oldState, inState, states.withDefaultValue(initialState))
          val retroactiveWork = newNodes.filter(n => inputs(n).exists(states.contains(_)))
          val (newStates, newWork) = if(states.contains(node) && oldState == newState) {
            (states, retroactiveWork.foldLeft(rest)(_.enqueue(_)))
          } else {
            (states + (node -> newState), (outputs(node) ++ retroactiveWork).foldLeft(rest)(_.enqueue(_)))
          }
          process(newWork, newStates)
        }
        case None => states
      }
    }
    process(initialNodes.to[Queue], HashMap())
  }

  def outputs(node: NodeT): Set[NodeT]
  def inputs(node: NodeT): Set[NodeT]

  def transfer(node: NodeT, old: StateT, inState: StateT, states: StateMap): (StateT, Set[NodeT])
  def combine(a: StateT, b: StateT): StateT
}

trait AnalyzerWithAutoOutputs extends Analyzer {
  val inputsMap: collection.Map[NodeT, Set[NodeT]]

  lazy val outputsMap: collection.Map[NodeT, Set[NodeT]] = {
    import scala.collection.mutable
    val map = mutable.HashMap[NodeT, mutable.HashSet[NodeT]]()
    for((n, ins) <- inputsMap; in <- ins) {
      val s = map.getOrElseUpdate(in, mutable.HashSet[NodeT]())
      s += n
    }
    map
  }

  final def outputs(node: NodeT): Set[NodeT] = outputsMap.getOrElse(node, Set())
  final def inputs(node: NodeT): Set[NodeT] = inputsMap.getOrElse(node, Set())
}
