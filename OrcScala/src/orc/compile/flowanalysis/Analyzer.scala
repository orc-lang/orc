package orc.compile.flowanalysis

import scala.collection.immutable.Queue
import scala.annotation.tailrec
import scala.collection.immutable.HashMap
import collection.Set
import scala.reflect.ClassTag
import orc.compile.Logger

//trait Node[T <: Node[T]] {
//  def outputs: Set[T]
//  def inputs: Set[T]
//}

//trait AnalysisValue[T <: AnalysisValue[T]] {
//  def combine(v: T): T
//}

abstract class Analyzer {
  type NodeT
  type EdgeT
  type StateT
  type StateMap = Map[NodeT, StateT]

  case class ConnectedNode(edge: EdgeT, node: NodeT)

  /** The set of nodes to initially process.
    */
  def initialNodes: Set[NodeT]
  /** The starting state of all the nodes and the default input state if there are no inputs selected.
    *
    * This value does not need to be an identity of combine.
    */
  val initialState: StateT

  class States(node: NodeT, states: StateMap) {
    private def ins: Set[ConnectedNode] = inputs(node)

    def apply(n: NodeT): StateT = {
      require(ins.exists(_.node == n), s"Analysis requested state for non-input node. At $node, requested state of $n (inputs are $ins).")
      states.getOrElse(n, initialState)
    }

    def get(n: NodeT): Option[StateT] = {
      require(ins.exists(_.node == n), s"Analysis requested state for non-input node. At $node, requested state of $n (inputs are $ins).")
      states.get(n)
    }

    def inState[T <: EdgeT: ClassTag](): StateT = {
      inStateReduced[T](combine _)
    }

    def inStateReduced[T <: EdgeT: ClassTag](f: (StateT, StateT) => StateT): StateT = {
      inStateProcessed[T, StateT](initialState, s => s, f)
    }

    def inStateProcessed[T <: EdgeT: ClassTag, R](initial: R, extract: StateT => R, f: (R, R) => R): R = {
      val TType = implicitly[ClassTag[T]]
      val insT = ins.collect { case cn @ ConnectedNode(TType(_), _) => cn }
      val inVals = insT.toSeq.map(cn => states.get(cn.node).map(extract).getOrElse(initial))
      val in = inVals.reduceOption(f).getOrElse(initial)
      in
    }
  }

  def apply(): StateMap = {
    @tailrec
    def process(work: Queue[NodeT], states: StateMap): StateMap = {
      work.dequeueOption match {
        case Some((node, rest)) => {
          //val ins = inputs(node)
          //val inState = ins.map(states.getOrElse(_, initialState)).fold(initialState)(combine)
          val oldState = states.getOrElse(node, initialState)
          val (newState, newNodes) = transfer(node, oldState, new States(node, states))
          val retroactiveWork = newNodes.filter(n => inputs(n).map(_.node).exists(states.contains(_)))
          val (newStates, newWork) = if (states.contains(node) && oldState == newState) {
            (states, retroactiveWork.foldLeft(rest)(_.enqueue(_)))
          } else {
            (states + (node -> newState), (outputs(node).map(_.node) ++ retroactiveWork).foldLeft(rest)(_.enqueue(_)))
          }
          //Logger.fine(s"Processed $node:    Queue: $rest => $newWork")
          assert(moreCompleteOrEqual(newState, oldState), s"The new state $newState > $oldState at $node")
          process(newWork, newStates)
        }
        case None => states
      }
    }
    process(initialNodes.to[Queue], HashMap())
  }

  def outputs(node: NodeT): Set[ConnectedNode]
  def inputs(node: NodeT): Set[ConnectedNode]

  def transfer(node: NodeT, old: StateT, states: States): (StateT, Set[NodeT])
  def combine(a: StateT, b: StateT): StateT
  def moreCompleteOrEqual(a: StateT, b: StateT): Boolean
}
