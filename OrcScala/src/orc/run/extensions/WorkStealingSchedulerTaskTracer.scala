//
// WorkStealingSchedulerTaskTracer.scala -- Trace processing for WorkStealingScheduler
// Project OrcScala
//
// Created by amp on Nov, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import java.io.OutputStreamWriter

import scala.collection.mutable.LongMap
import scala.collection.mutable.BitSet
import scala.collection.mutable.ArrayBuffer

import orc.util.{CsvWriter, ExecutionLogOutputStream}

object WorkStealingSchedulerTaskTracer {
  class Task(val id: Long) {
    var outEdges = ArrayBuffer[Task]()
    
    var realStartTime: Long = Long.MinValue
    
    var realEndTime: Long = Long.MinValue
    
    var threadStartTime: Long = Long.MinValue
    
    var threadEndTime: Long = Long.MinValue
    
    def length = threadLength
    
    def threadLength = if (threadStartTime != Long.MinValue && threadEndTime != Long.MinValue) threadEndTime - threadStartTime else 0
    def realLength = if (realStartTime != Long.MinValue && realEndTime != Long.MinValue) realEndTime - realStartTime else 0
    
    var idealStartTime: Long = Long.MinValue
    
    def idealEndTime = idealStartTime + length
    
    override def hashCode() = id.toInt
    override def toString() = s"Task($id, $realStartTime, $idealStartTime, $length; $threadStartTime, $threadEndTime)"
    override def equals(o: Any): Boolean = o match {
      case t: Task =>
        id == t.id
      case _ => 
        false
    }
  }
  
  /*
  type AdjacencyList = HashMap[Long, HashSet[Task]]
  def newAdjacencyList(): AdjacencyList = new AdjacencyList // ((_) => new ArrayBuffer[Task]())
  */
  
  type TaskList = LongMap[Task]
  def newTaskList(): TaskList = new TaskList
  
  class Graph(val tasks: TaskList = newTaskList(), 
      //val inEdges: AdjacencyList = newAdjacencyList(),
      //val outEdges: AdjacencyList = newAdjacencyList()
      ) {
    def apply(id: Long): Task = tasks.getOrElseUpdate(id, new Task(id)) 
    
    def +=(t: Task): Unit = {
      require(!tasks.contains(t.id) || (tasks(t.id) eq t))
      tasks += t.id -> t
    }
    
    def +=(e: (Task, Task)): Unit = {
      val (source, dest) = e
      if(!(source.outEdges contains dest)) {
        this += source
        this += dest
        //inEdges.getOrElseUpdate(dest.id, new HashSet[Task]()) += source
        source.outEdges.sizeHint(source.outEdges.size + 1)
        source.outEdges += dest
      }
    }
    
    def removeZeroLength(): Unit = {
      val idOffset = tasks.keys.min
      def toMark(i: Long) = (i - idOffset).toInt
      def fromMark(i: Int) = i.toLong + idOffset
      
      val unmarked = new BitSet(tasks.size)
      unmarked ++= tasks.keysIterator.map(toMark)
      var rem = unmarked.size
      
      var i = 0
      
      /*
      def cleanOutgoing(n: Task): Unit = {
        if (unmarked contains toMark(n.id)) {
          unmarked -= toMark(n.id)
          rem -= 1
          for (m <- n.outEdges) {
            cleanOutgoing(m)
          }
          
          if (i % 10000 == 0) {
            println(s"removeZeroLength processed $i tasks.")
          }
          i += 1
          
          val oldOutEdges = n.outEdges.clone()
          
          n.outEdges.clear()
          
          if (n.length == 0) {
            tasks -= n.id
          }
          
          for (m <- oldOutEdges) {
            if (m.length == 0) {
              n.outEdges ++= m.outEdges
            } else {
              n.outEdges += m
            }
          }
        }
      }
      
      var j = 0
      while(rem > 0) {
        if (j % 1000 == 0) {
          println(s"Starting removeZeroLength step with ${tasks.size} tasks remaining.")
        }
        j -= 1
        cleanOutgoing(tasks(fromMark(unmarked.last)))
      }
      */
      
      while(rem > 0) {
        //println(s"Starting sort step with ${unmarked.size} tasks remaining.")
        val n = this(fromMark(unmarked.head))
        
        unmarked -= toMark(n.id)
        rem -= 1
        var stack: List[(Task, Iterator[Task])] = List((n, n.outEdges.iterator))
        
        while(stack.nonEmpty) {
          val n = stack.head._1
          val it: Iterator[Task] = stack.head._2
          
          if(it.hasNext) {
            val m = it.next()
            
            if(unmarked.contains(toMark(m.id))) {
              unmarked -= toMark(m.id)
              //println(m)
              rem -= 1
              stack ::= ((m, m.outEdges.iterator))
            }
          } else {
            if (i % 10000 == 0) {
              //println(s"removeZeroLength processed $i tasks.")
            }
            i += 1
            
            val oldOutEdges = n.outEdges.clone()
            
            n.outEdges.clear()
            
            if (n.length == 0) {
              tasks -= n.id
            }
            
            for (m <- oldOutEdges) {
              if (m.length == 0) {
                n.outEdges ++= m.outEdges
              } else {
                n.outEdges += m
              }
            }
            stack = stack.tail
          }
        }
      }
      
      tasks.repack()
    }
    
    def topologicalOrder: Seq[Task] = {
      val idOffset = tasks.keys.min
      def toMark(i: Long) = (i - idOffset).toInt
      def fromMark(i: Int) = i.toLong + idOffset
      
      val result = new ArrayBuffer[Task]
      result.sizeHint(tasks.size)
      
      val unmarked = new BitSet(tasks.size)
      unmarked ++= tasks.keysIterator.map(toMark)
      var rem = unmarked.size

      var i = 0

      while(rem > 0) {
        //println(s"Starting sort step with ${unmarked.size} tasks remaining.")
        val n = this(fromMark(unmarked.head))
        
        unmarked -= toMark(n.id)
        rem -= 1
        var stack: List[(Task, Iterator[Task])] = List((n, n.outEdges.iterator))
        
        while(stack.nonEmpty) {
          val n = stack.head._1
          val it: Iterator[Task] = stack.head._2
          
          if(it.hasNext) {
            val m = it.next()
        
            if (i % 10000 == 0) {
              //println(s"Toposort processed $i tasks.")
            }
            i += 1
            
            if(unmarked.contains(toMark(m.id))) {
              unmarked -= toMark(m.id)
              rem -= 1
              stack ::= ((m, m.outEdges.iterator))
            }
          } else {
            result += n
            stack = stack.tail
          }
        }
      }
      
      result.reverse
    }
    
    def assignIdealStarts(): Unit = {
      val topo = topologicalOrder
      var i = 0
      //topo.foreach(t => println(t.id))
      for (n <- topo) {
        val outs = n.outEdges
        n.idealStartTime = n.idealStartTime max 0
        for (o <- outs) {
          o.idealStartTime = o.idealStartTime max 0 max n.idealEndTime
        }
        
        if (i % 10000 == 0) {
          //println(s"Set starts for $i tasks.")
        }
        i += 1
      }
    } 
  }
  
  object Graph {
    def fromTrace(buffers: Traversable[orc.util.Tracer.TraceBuffer]): Graph = {
      val g = new Graph()
      
      println(s"Loading graph from ${buffers.map(_.eventsInBuffer).sum} events (${buffers.size} buffers).")

      for (buffer <- buffers) {
        buffer.locationIds = null
        buffer.millitimes = null
      }

      for (buffer <- buffers.toStream) {
        buffer synchronized {
          for (i <- 0 to buffer.eventsInBuffer - 1) {
            import SimpleWorkStealingScheduler.{TaskParent, TaskStart, TaskEnd}
            buffer.typeIds(i) match {
              case TaskParent if buffer.fromArgs(i) >= 0 && buffer.toArgs(i) >= 0 =>
                val parent = g(buffer.fromArgs(i))
                val child = g(buffer.toArgs(i))
                g += parent -> child
              case TaskStart if buffer.fromArgs(i) >= 0 =>
                g(buffer.fromArgs(i)).realStartTime = buffer.nanotimes(i)
                g(buffer.fromArgs(i)).threadStartTime = buffer.toArgs(i)
              case TaskEnd if buffer.fromArgs(i) >= 0 =>
                g(buffer.fromArgs(i)).realEndTime = buffer.nanotimes(i)
                g(buffer.fromArgs(i)).threadEndTime = buffer.toArgs(i)
              case _ => ()
            }
          }
          //println(s"Loaded ${g.tasks.size} tasks. Last buffer had ${buffer.eventsInBuffer} events.")
          buffer.dispose()
        }
      }
      
      g
    }
  }
  
  def dumpSchedule(suffix: String) = synchronized {
    val g = Graph.fromTrace(orc.util.Tracer.takeBuffers())
    val csvOut = ExecutionLogOutputStream(s"schedule_$suffix", "csv", "Schedule output file")
    if (csvOut.isDefined) {
      println(s"Eliminating zero-length. (${g.tasks.size})")
      g.removeZeroLength()
      println(s"Assigning ideal starts. (${g.tasks.size})")
      g.assignIdealStarts()
      println(s"Writing output file. (${g.tasks.size})")
      val timeZero = g.tasks.valuesIterator.filter(t => t.realStartTime != Long.MinValue && t.outEdges.nonEmpty).map(_.realStartTime).min
      
      val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
      val csvWriter = new CsvWriter(traceCsv.append(_))
      val tableColumnTitles = Seq(
          //"Task ID [id]",
          "Real Start Time [realStart]",
          "Real End Time [realEnd]",
          "Ideal Start Time [idealStart]",
          "Ideal End Time [idealEnd]",
          //"Length [length]"
          )
      csvWriter.writeHeader(tableColumnTitles)
      csvWriter.writeRows(g.tasks.valuesIterator.map(t => 
        (
            //t.id, 
            if (t.realStartTime == Long.MinValue) "" else t.realStartTime - timeZero, 
            if (t.realEndTime == Long.MinValue) "" else t.realEndTime - timeZero, 
            t.idealStartTime, 
            t.idealEndTime, 
            //t.length
            )))
      traceCsv.close()
    }
  }
}