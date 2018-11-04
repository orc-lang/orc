//
// Analyzer.scala -- Scala object and abstract class Analyzer and trait AnalyzerEdgeCache
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.flowanalysis

import scala.collection.mutable
import scala.annotation.tailrec
import scala.reflect.ClassTag
import orc.compile.Logger
import java.io.File
import java.util.logging.Level
import java.io.FileWriter
import orc.compile.orctimizer.FlowGraph

object Analyzer {
  val checkAnalysis = false

}

abstract class Analyzer {
  import Analyzer._

  type NodeT
  type EdgeT
  type StateT
  type StateMap = collection.Map[NodeT, StateT]

  case class ConnectedNode(edge: EdgeT, node: NodeT)

  case class AnalyzerLogEntry(id: Int, node: NodeT, inEdges: Seq[EdgeT], newState: StateT, isChange: Boolean, tracePredicate: (AnalyzerLogEntry, StateMap) => Boolean)

  val defaultTracePredicate = (e: AnalyzerLogEntry, s: StateMap) => false

  val tracePredicateVariable = new ThreadLocal[(AnalyzerLogEntry, StateMap) => Boolean]() {
    override def initialValue() = defaultTracePredicate
  }

  val level = if (checkAnalysis) Level.INFO else Level.FINER
  val finerLevel = if (checkAnalysis) Level.FINER else Level.FINEST


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
      val insT = ins.view.collect { case cn @ ConnectedNode(TType(_), _) => cn }
      val inVals = insT.map(cn => states.get(cn.node).map(extract).getOrElse(initial))
      val in = inVals.reduceOption(f).getOrElse(initial)
      //println((TType, ins, insT, inVals, in))
      in
    }
  }

  protected def tracePredicate(f: (AnalyzerLogEntry, StateMap) => Boolean) = {
    tracePredicateVariable.set(f)
  }

  def apply(): StateMap = {
    var resultStates: StateMap = Map()
    var id = 0
    val traversal = mutable.Buffer[AnalyzerLogEntry]()
    var avoidedEnqueues = 0
    val startTime = System.nanoTime()

    Logger.log(finerLevel, s"Traversal $this: starting")

    try {
      val queue = mutable.Queue[NodeT]()
      val queueContent = mutable.Set[NodeT]()
      val states: mutable.Map[NodeT, StateT] = mutable.HashMap()
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
      def process(): StateMap = {
        smartDequeue() match {
          case Some(node) => {
            if(Logger.julLogger.isLoggable(level)) {
              tracePredicateVariable.set(defaultTracePredicate)
            }

            val oldState = states.getOrElse(node, initialState)
            val (newState, newNodes) = transfer(node, oldState, new States(node, states))
            val retroactiveWork = newNodes.filter(n => inputs(n).map(_.node).exists(states.contains(_)))
            if (oldState == newState && states.contains(node)) {
              retroactiveWork.foreach(smartEnqueue)
            } else {
              outputs(node).map(_.node).foreach(smartEnqueue)
              retroactiveWork.foreach(smartEnqueue)
              states += (node -> newState)
            }

            if(Logger.julLogger.isLoggable(level)) {
              id += 1
              traversal += AnalyzerLogEntry(id, node, inputs(node).map(_.edge), newState, oldState == newState, tracePredicateVariable.get())
            }

            if (checkAnalysis) {
              assert(moreCompleteOrEqual(newState, oldState),
                  s"The new state (at $node)\n$newState\n is not a refinement of the old state \n$oldState")
              assert(!(moreCompleteOrEqual(newState, oldState) && moreCompleteOrEqual(oldState, newState) && oldState != newState),
                  s"The states below are mutually more complete, but not equal\n$newState\n====\n$oldState")
            }

            process()
          }
          case None => states
        }
      }

      initialNodes.foreach(smartEnqueue)
      resultStates = process()
      resultStates
    } finally {
      val endTime = System.nanoTime()
      if(Logger.julLogger.isLoggable(level)) {
        writeSelectedLog(traversal, resultStates)
      }
      if(Logger.julLogger.isLoggable(finerLevel)) {
        writeFullLog(traversal, startTime, endTime, avoidedEnqueues)
      }
    }
  }

  private def writeSelectedLog(traversal: mutable.Buffer[AnalyzerLogEntry], states: StateMap) = {
    import orc.util.PrettyPrintInterpolator
    import orc.util.FragmentAppender

    class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
      implicit def implicitInterpolator(sc: StringContext) = new MyInterpolator(sc)
      class MyInterpolator(sc: StringContext) extends Interpolator(sc) {
        override val processValue: PartialFunction[Any, FragmentAppender] = PartialFunction.empty
      }
    }
    val interpolator = new MyPrettyPrintInterpolator
    import interpolator._

    def findNodeEntryBefore(n: NodeT, i: Int): (Int, AnalyzerLogEntry) = {
      val ind = traversal.lastIndexWhere(_.node == n, i)
      if (ind < 0)
        (-1, AnalyzerLogEntry(-1, n, Seq(), initialState, false, defaultTracePredicate))
      else
        (ind, traversal(ind))
    }

    val statesDefault = states.toMap.withDefaultValue(initialState)

    val selected = traversal.zipWithIndex.par.flatMap({ p =>
      val (e, i) = p
      if (e.tracePredicate(e, statesDefault)) {
        def print(e: AnalyzerLogEntry, i: Int, depth: Int): FragmentAppender = {
          if (depth < 10) {
            FragmentAppender.mkString(inputs(e.node).map({
              case ConnectedNode(_, n) =>
                val (j, e1) = findNodeEntryBefore(n, i)
                if(j < 0) {
                  pp""
                } else {
                  val in = ">" * depth
                  val out = "<" * depth
                  pp"NODE: ${e.node}\nSTATE: ${e.newState}\n$in$StartIndent\n${print(e1, j, depth+1)}$EndIndent\n$out"
                }
            }), "\n")
          } else {
            pp"..."
          }
        }
        Seq("\n\n" + print(e, e.id, 0))
      } else {
        Seq()
      }
    }).seq

    if (selected.nonEmpty) {
      val traceFile = File.createTempFile("analysis_selected_log_", ".txt")

      val out = new FileWriter(traceFile)
      for(l <- selected) {
        out.write(l)
        out.write("\n")
      }
      out.close()

      Logger.log(level, s"Traversal $this: selected trace in $traceFile")
    } else {
      //Logger.log(level, s"Traversal $this: selected trace is empty")
    }
  }

  private def writeFullLog(traversal: mutable.Buffer[AnalyzerLogEntry], startTime: Long, endTime: Long, avoidedEnqueues: Int) = {
    def entryToString(t: AnalyzerLogEntry): String = {
      val AnalyzerLogEntry(id, n, ins, s, b, _) = t
      f"$id% 4d: ${if (b) "==" else "!="} $s%s\n    ins=[${ins.mkString(",\n         ")}%s]"
    }
    def traversalTable = traversal.groupBy(_.node).par.map({
      case (n, b) =>
        val nn = n match {
          case n: FlowGraph.WithSpecificAST => n.location
          case n: FlowGraph.Node => n.ast
          case _ => ""
        }
        (b, s"========= Node $n @\n$nn\n=====\n${b.map(entryToString).mkString("\n")}")
    }).seq.toSeq.sortBy(_._1.map(_.id).max).map(_._2)
    val traceFile = File.createTempFile("analysis_full_log", ".txt")

    val out = new FileWriter(traceFile)
    for(l <- traversalTable) {
      out.write(l)
      out.write("\n")
    }
    out.close()

    Logger.log(level, s"Traversal $this: (${((endTime.toDouble - startTime) / 1000 / 1000).formatted("%.1f")} ms) ${traversal.map(_.node).toSet.size} nodes, ${traversal.flatMap(_.inEdges).toSet.size} edges, ${traversal.size} visits ($avoidedEnqueues eliminated), trace in $traceFile")
  }

  def outputs(node: NodeT): Seq[ConnectedNode]
  def inputs(node: NodeT): Seq[ConnectedNode]

  def transfer(node: NodeT, old: StateT, states: States): (StateT, Seq[NodeT])
  def moreCompleteOrEqual(a: StateT, b: StateT): Boolean
}

trait AnalyzerEdgeCache extends Analyzer {
    val inputsCache = collection.mutable.HashMap[NodeT, Seq[ConnectedNode]]()

    def inputsCompute(node: NodeT): collection.Seq[ConnectedNode]

    final def inputs(node: NodeT): collection.Seq[ConnectedNode] = {
      inputsCache.getOrElseUpdate(node, inputsCompute(node))
    }

    val outputsCache = collection.mutable.HashMap[NodeT, Seq[ConnectedNode]]()

    def outputsCompute(node: NodeT): collection.Seq[ConnectedNode]

    final def outputs(node: NodeT): collection.Seq[ConnectedNode] = {
      outputsCache.getOrElseUpdate(node, outputsCompute(node))
    }
}
