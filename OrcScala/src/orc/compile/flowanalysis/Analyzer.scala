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

object Analyzer {
  val checkAnalysis = false
}

abstract class Analyzer {
  import Analyzer.checkAnalysis

  type NodeT
  type EdgeT
  type StateT
  type StateMap = Map[NodeT, StateT]

  case class ConnectedNode(edge: EdgeT, node: NodeT)

  /** The set of nodes to initially process.
    */
  def initialNodes: Seq[NodeT]

  /** The starting state of all the nodes and the default input state if there are no inputs selected.
    */
  val initialState: StateT

  class States(node: NodeT, states: StateMap) {
    private def ins: Seq[ConnectedNode] = inputs(node)

    private def checkNode(n: NodeT): Unit = {
      if (checkAnalysis)
        require(ins.exists(_.node == n), s"Analysis requested state for non-input node. At $node, requested state of $n (inputs are $ins).")
    }

    def apply(n: NodeT): StateT = {
      checkNode(n)
      states.getOrElse(n, initialState)
    }

    def get(n: NodeT): Option[StateT] = {
      checkNode(n)
      states.get(n)
    }

    /** Reduce all incoming states using f.
      *
      * Use initialState for any missing inputs or as output if there are no input edges of the specified type.
      */
    def inStateReduced[T <: EdgeT: ClassTag](f: (StateT, StateT) => StateT): StateT = {
      inStateProcessed[T, StateT](initialState, s => s, f)
    }

    /** For each incoming edges of type T, extract a value of type R from the state on that edges and then reduce those values.
      *
      * For any inputs which are not computed or if there are no inputs at all use initial.
      */
    def inStateProcessed[T <: EdgeT: ClassTag, R](initial: R, extract: StateT => R, f: (R, R) => R): R = {
      val TType = implicitly[ClassTag[T]]
      val insT = ins.collect { case cn @ ConnectedNode(TType(_), _) => cn }
      val inVals = insT.map(cn => states.get(cn.node).map(extract).getOrElse(initial))
      val in = inVals.reduceOption(f).getOrElse(initial)
      //println((TType, ins, insT, inVals, in))
      in
    }
  }

  def apply(): StateMap = {
    var id = 0
    val traversal = mutable.Buffer[(Int, NodeT, Seq[EdgeT], StateT, Boolean)]()
    var avoidedEnqueues = 0
    val startTime = System.nanoTime()

    val level = if (checkAnalysis) Level.INFO else Level.FINER
    Logger.log(level, s"Traversal $this: starting")

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

            if(Logger.julLogger.isLoggable(level)) {
              id += 1
              traversal += ((id, node, inputs(node).map(_.edge), newState, oldState == newState))
            }

            if (checkAnalysis) {
              assert(moreCompleteOrEqual(newState, oldState),
                  s"The new state (at $node)\n$newState\n is not a refinement of the old state \n$oldState")
              assert(!(moreCompleteOrEqual(newState, oldState) && moreCompleteOrEqual(oldState, newState) && oldState != newState),
                  s"The states below are mutually more complete, but not equal\n$newState\n====\n$oldState")
            }

            process(newStates)
          }
          case None => states
        }
      }

      initialNodes.foreach(smartEnqueue)
      process(HashMap())
    } finally {
      val endTime = System.nanoTime()
      if(Logger.julLogger.isLoggable(level)) {
        def entryToString(t: (Int, NodeT, Seq[EdgeT], StateT, Boolean)): String = {
          val (id, n, ins, s, b) = t
          f"$id% 4d: ${if (b) "==" else "!="} $s%s\n    ins=[${ins.mkString(",\n         ")}%s]"
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
        val traceFile = File.createTempFile("analysis", ".txt")

        Logger.log(level, s"Traversal $this: (${((endTime.toFloat - startTime) / 1000 / 1000).formatted("%.1f")} ms) ${traversal.map(_._2).toSet.size} nodes, ${traversal.flatMap(_._3).toSet.size} edges, ${traversal.size} visits ($avoidedEnqueues eliminated), trace in $traceFile")

        val out = new FileWriter(traceFile)
        for(l <- traversalTable) {
          out.write(l)
          out.write("\n")
        }
        out.close()
      }
    }
  }

  def outputs(node: NodeT): Seq[ConnectedNode]
  def inputs(node: NodeT): Seq[ConnectedNode]

  def transfer(node: NodeT, old: StateT, states: States): (StateT, Seq[NodeT])
  def moreCompleteOrEqual(a: StateT, b: StateT): Boolean
}
