package orc.compile.flowanalysis

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.annotation.tailrec
import scala.collection.immutable.HashMap
import collection.Set
import scala.reflect.ClassTag
import orc.compile.Logger
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.util.logging.Level
import java.io.FileWriter
import orc.compile.orctimizer.FlowGraph

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

    /*def inState[T <: EdgeT: ClassTag](): StateT = {
      inStateReduced[T](combine _)
    }*/

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
    var id = 0
    val traversal = mutable.Buffer[(Int, NodeT, Set[EdgeT], StateT)]()
    var avoidedEnqueues = 0 

    try {
      val queue = mutable.Queue[NodeT]()
      val queueContent = mutable.Set[NodeT]()
      def smartEnqueue(n: NodeT): Unit = {
        if (! queueContent.contains(n)) {
          queueContent += n
          queue.enqueue(n)
        } else {
          avoidedEnqueues += 1
        }
      }
      def smartDequeue(): Option[NodeT] = {
        try {
          val n = queue.dequeue()
          queueContent -= n
          Some(n)
        } catch {
          case _: NoSuchElementException => 
            None
        }
      }
      
      @tailrec
      def process(states: StateMap): StateMap = {
        smartDequeue() match {
          case Some(node) => {
            val oldState = states.getOrElse(node, initialState)
            val (newState, newNodes) = transfer(node, oldState, new States(node, states))
            val retroactiveWork = newNodes.filter(n => inputs(n).map(_.node).exists(states.contains(_)))
            val newStates = if (states.contains(node) && oldState == newState) {
              retroactiveWork.foreach(smartEnqueue)
              states
            } else {
              outputs(node).map(_.node).foreach(smartEnqueue)
              retroactiveWork.foreach(smartEnqueue)
              states + (node -> newState)
            }

            id += 1
            traversal += ((id, node, inputs(node).map(_.edge), newState))

            assert(moreCompleteOrEqual(newState, oldState), s"The new state (at $node)\n$newState\n is not a refinement of the old state \n$oldState")
            process(newStates)
          }
          case None => states
        }
      }
      
      initialNodes.foreach(smartEnqueue)
      process(HashMap())
    } finally {
      def entryToString(t: (Int, NodeT, Set[EdgeT], StateT)): String = {
        val (id, n, ins, s) = t
        f"$id% 4d: $s%s\n    ins=[${ins.mkString(",\n         ")}%s]"
      }
      def traversalTable = traversal.groupBy(_._2).par.map({
        case (n, b) =>
          val nn = n match { 
            case n: FlowGraph.WithSpecificAST => n.location
            case n: FlowGraph.Node => n.ast
            case _ => ""
          }
          (b, s"========= Node $n @\n$nn\n=====\n${b.map(entryToString).mkString("\n")}")
      }).seq.toSeq.sortBy(_._1.map(_._1).max).map(_._2)
      lazy val traceFile = File.createTempFile("analysis", ".txt");
      Logger.fine(s"Traversal: ${traversal.map(_._2).toSet.size} nodes, ${traversal.size} visits ($avoidedEnqueues eliminated), trace in $traceFile")
      if(Logger.julLogger.isLoggable(Level.FINE)) {
        val out = new FileWriter(traceFile)
        for(l <- traversalTable) {
          out.write(l)
          out.write("\n")
        }
        out.close()
      }
    }
  }

  def outputs(node: NodeT): Set[ConnectedNode]
  def inputs(node: NodeT): Set[ConnectedNode]

  def transfer(node: NodeT, old: StateT, states: States): (StateT, Set[NodeT])
  def combine(a: StateT, b: StateT): StateT
  def moreCompleteOrEqual(a: StateT, b: StateT): Boolean
}
