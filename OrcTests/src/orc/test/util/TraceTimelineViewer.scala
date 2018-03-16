//
// TraceTimelineViewer.scala -- Scala object TraceTimelineViewer
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.util

import collection.JavaConverters._
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.awt.geom.Point2D
import java.awt.geom.Line2D
import java.awt.Color
import java.awt.Dimension
import java.awt.BasicStroke
import javax.swing.WindowConstants
import java.awt.Font
import java.awt.event.MouseWheelEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.SwingUtilities
import scala.collection.immutable.SortedMap
import scala.collection.immutable.LongMap
import java.awt.geom.Ellipse2D

object TraceTimelineViewer {
  implicit class DurationAdds(d: Duration) {
    def toDoubleSeconds: Double = d.getSeconds + d.getNano / (1000.0 * 1000 * 1000)
    def min(o: Duration): Duration = if (d.compareTo(o) <= 0) d else o
    def max(o: Duration): Duration = if (d.compareTo(o) >= 0) d else o
  }
  
  object DurationAdds {
    def apply(d: Double) = {
      Duration.ofSeconds(d.toLong, ((d - d.toLong) * 1000 * 1000 * 1000).toLong)
    }
  }
  
  class Node(val id: Long, val start: Duration, val end: Duration, val tpe: SchedulableExecutionType, val thread: Int) {
    def duration = end.minus(start)
    
    val hasBox: Boolean = true
    val label: String = ""
    
    def copy(id: Long = id, start: Duration = start, end: Duration = end, tpe: SchedulableExecutionType = tpe, thread: Int = thread) = {
      val outer = this
      new Node(id, start, end, tpe, thread) {
        override val hasBox = outer.hasBox
        override val label = outer.label
      }
    }
  }
  implicit val NodeOrdering: Ordering[Node] = Ordering.by(_.start) 
  case class Edge(time: Duration, source: Long, target: Long)
  implicit val EdgeOrdering: Ordering[Edge] = Ordering.by(_.time) 
  
  case class Timeline(nodes: IndexedSeq[Node], edges: IndexedSeq[Edge]) {
    val nodesById = nodes.map(n => n.id -> n).toMap
    
    private[this] val nodeIndiciesByStart = SortedMap[Duration, Int]() ++ nodes.zipWithIndex.map({ case (n, i) => n.start -> i })
    private[this] val nodeIndiciesByEnd = SortedMap[Duration, Int]() ++ nodes.zipWithIndex.reverse.map({ case (n, i) => n.end -> i })
    
    def nodeRange(a: Duration, b: Duration) = {
      // Get index of first node with end > a
      //nodes.indexWhere(_.end > a)
      val left = try nodeIndiciesByEnd.valuesIteratorFrom(a).next() catch {
        case _: NoSuchElementException => nodes.size
      }
      // Get index of last node with start < b
      //nodes.lastIndexWhere(_.start < b)
      val right = try nodeIndiciesByStart.valuesIteratorFrom(b).next() catch {
        case _: NoSuchElementException => nodes.size
      }
      
      nodes.view(left, right)
    }
    
    private[this] val edgeIndiciesByStart = SortedMap[Duration, Int]() ++ edges.zipWithIndex.map({ case (e, i) => nodesById(e.source).start -> i })
    private[this] val edgeIndiciesByEnd = SortedMap[Duration, Int]() ++ edges.zipWithIndex.reverse.map({ case (e, i) => nodesById(e.target).start -> i })

    def edgeRange(a: Duration, b: Duration) = {
      // Get index of first edge with end > a
      val left = try edgeIndiciesByEnd.valuesIteratorFrom(a).next() catch {
        case _: NoSuchElementException => nodes.size
      }
      // Get index of last edge with start < b
      val right = try edgeIndiciesByStart.valuesIteratorFrom(b).next() catch {
        case _: NoSuchElementException => nodes.size
      }
      
      edges.view(left, right)
    }
    
    private[this] val edgesByTime = SortedMap[(Duration, Long, Long), Edge]() ++ edges.map(e => (e.time, e.source, e.target) -> e)

    def edgeTimeRange(a: Duration, b: Duration) = {
      edgesByTime.range((a, 0, 0), (b, Long.MaxValue, Long.MaxValue)).values
    }
    
    def inDegree(n: Node): Long = inDegree(n.id)
    val inDegree = edges.foldLeft(LongMap[Int]())((m, e) => m.updated(e.target, m.getOrElse(e.target, 0) + 1)).withDefaultValue(0)
    
    def outDegree(n: Node): Long = outDegree(n.id)
    val outDegree = edges.view.filter(e => nodesById(e.target).tpe == SchedulerExecution).foldLeft(LongMap[Int]())((m, e) => m.updated(e.source, m.getOrElse(e.source, 0) + 1)).withDefaultValue(0)
    
    val minThread = nodes.map(_.thread).min
    val maxThread = nodes.map(_.thread).max
    
    val minTime = nodes.head.start min edges.head.time
    val maxTime = nodes.last.end max edges.last.time
  }

  
  sealed abstract class SchedulableExecutionType(val id: Long)
  case object SchedulerExecution extends SchedulableExecutionType(0)
  case object StackExecution extends SchedulableExecutionType(1)
  case object InlineExecution extends SchedulableExecutionType(2)
  case object DataItemExecution extends SchedulableExecutionType(3)
  
  object SchedulableExecutionType {
    def apply(i: Long): SchedulableExecutionType = {
      i match {
        case 0 => SchedulerExecution
        case 1 => StackExecution
        case 2 => InlineExecution
      }
    }
  }
  
  def loadTimeline(inputFilename: String): Timeline = {
    val reader = Files.newBufferedReader(Paths.get(inputFilename))
    val lines = reader.lines().iterator.asScala
    var lastTask = 0L
    var lastBufSt = 0L
    
    val nodes = collection.mutable.HashMap[Long, Node]()
    val edges = collection.mutable.Buffer[Edge]()
    
    var minTime = Duration.ofNanos(Long.MaxValue)
    
    val threadIdMap = collection.mutable.HashMap[Int, Int]()
    
    def getCompactThreadID(id: Int): Int = {
      threadIdMap.getOrElseUpdate(id, (threadIdMap.values.toSeq :+ -1).max + 1)
    }
        
    for (l <- lines.drop(1)) {
      val Array(_, timeS, threadS, sourceS, tpe, fromS, toS) = l.split(",")
      val (time, thread, source, from, to) = (Duration.ofNanos(timeS.toLong), getCompactThreadID(threadS.toInt), sourceS.toInt, fromS.toLong, toS.toLong)
      minTime = minTime min time
      tpe match {
        case "TaskStrt" => {
          val spawnType = SchedulableExecutionType(source)
          nodes += from -> new Node(from, time, null, spawnType, thread)
          lastTask = from
        }
        case "TaskEnd" => {
          nodes.get(from).foreach(orig => {
            nodes += from -> orig.copy(end = time)
          })
        }
        case "TaskPrnt" => {
          edges += Edge(time, from, to)
        }
        case "Execute" => {
          nodes += -from -> new Node(-from, time, time, DataItemExecution, thread) {
            override val label = from.toString
            override val hasBox = false
          }
          edges += Edge(time, lastTask, -from)
        }
        case "NewBufSt" => {
          nodes += time.toNanos() -> new Node(time.toNanos(), time, null, DataItemExecution, thread) {
            override val label = "BufAlloc"
          }
          lastBufSt = time.toNanos()
        }
        case "NewBufEn" => {
          nodes.get(lastBufSt).foreach(orig => {
            nodes += lastBufSt -> orig.copy(end = time)
          })
        }
        case _ => ()
      }
    }

    val nodesMap = nodes.filter({ case (_, n) => n.end != null }).
      mapValues(n => n.copy(start = n.start.minus(minTime), end = n.end.minus(minTime)))

    val processedNodes = nodesMap.values
    val processedEdges = edges.view.
          filter(e => nodesMap.contains(e.source) && nodesMap.contains(e.target)).
          map(e => e.copy(time = e.time.minus(minTime)))
    
    Timeline(
        processedNodes.toArray.sortBy(_.start), 
        processedEdges.toArray.sortBy(e => nodesMap(e.source).start))
  }
  
  class TimelineDisplay(timeline: Timeline) extends JPanel {
    var zoom: Double = 10000000
    
    var showItems = true
    var showFansIns = true
    var showFansOuts = false
    var showEdges = false
    var showEdgeMarks = false
    
    var fastMode = true
    var renderEverything = false
            
    override def paintComponent(gRaw: Graphics): Unit = {
      val g = gRaw.asInstanceOf[Graphics2D]

      //println(s"${g.getClip}, ${g.getClipBounds}")
      
      
      val outsideTransform = g.getTransform
      g.scale(zoom, zoom)

      //println(s"${g.getClip}, ${g.getClipBounds}")

      val threadHeight = g.getClip.asInstanceOf[Rectangle2D].getHeight / (timeline.maxThread max 8)
      
      implicit class NodeAdds(n: Node) {
        def rect = {
          val x = n.start.toDoubleSeconds
          val y = n.thread * threadHeight
          val w = n.duration.toDoubleSeconds
          new Rectangle2D.Double(x, y, w, threadHeight * 0.95)
        }
        
        def startAnchor: Point2D.Double = {
          val x = n.start.toDoubleSeconds
          val y = n.thread * threadHeight
          new Point2D.Double(x, y + threadHeight/2)
        }
        
        def endAnchor: Point2D.Double = {
          val x = n.end.toDoubleSeconds
          val y = n.thread * threadHeight
          new Point2D.Double(x, y + threadHeight/2)
        }
        
        def topStartAnchor: Point2D.Double = {
          val x = n.start.toDoubleSeconds
          val y = n.thread * threadHeight
          new Point2D.Double(x, y)
        }
        
        def topAnchor: Point2D.Double = {
          val x = n.start.toDoubleSeconds + (n.end.toDoubleSeconds - n.start.toDoubleSeconds) / 2
          val y = n.thread * threadHeight
          new Point2D.Double(x, y)
        }
      }

      
      {
        g.setPaint(new Color(0.0f, 0.0f, 0.0f))
        import timeline._
        g.fill(new Rectangle2D.Double(
            minTime.toDoubleSeconds, minThread * threadHeight, 
            maxTime.toDoubleSeconds - minTime.toDoubleSeconds, (maxThread - minThread + 1) * threadHeight))
      }
      
      val boundingBox = new Rectangle2D.Double(timeline.minTime.toDoubleSeconds, timeline.minThread, 
          timeline.maxTime.toDoubleSeconds - timeline.minTime.toDoubleSeconds, (timeline.maxThread - timeline.minThread) * threadHeight)
      
      // Nodes
      
      g.setStroke(new BasicStroke((1.5 / zoom).toFloat))
      
      val mainClipRect = g.getClip.asInstanceOf[Rectangle2D.Double]
      
      val itemFont = new Font("Arial", 0, 1).deriveFont(threadHeight.toFloat / 7)
      val symbolFont = new Font("Arial Bold", 0, 1).deriveFont(threadHeight.toFloat)
      val countFont = new Font("Arial", 0, 1).deriveFont(threadHeight.toFloat / 8)
      
      val maxNodes = if (fastMode) 10000 else Int.MaxValue
      val maxEdges = if (fastMode) 3000 else Int.MaxValue
      
      val starts = DurationAdds(mainClipRect.getX - mainClipRect.getWidth*2)
      val ends = DurationAdds(mainClipRect.getX + mainClipRect.getWidth*3)
      val nodesToDraw = {
        val s = if (renderEverything) timeline.nodes else timeline.nodeRange(starts, ends)
        s.sliding(1, s.size / maxNodes max 1).map(_.head).toArray
      }

      nodesToDraw.filter(_.hasBox).foreach(n => {
        val rect = n.rect
        boundingBox.add(rect)
        g.setPaint(n.tpe match {
          case SchedulerExecution =>
            new Color(1.0f, 0.4f, 0.4f, 0.5f)
          case StackExecution =>
            new Color(0.9f, 0.9f, 0.0f, 0.4f)       
          case InlineExecution =>
            new Color(0.7f, 0.7f, 0.7f, 0.2f)
          case _ =>
            new Color(0.0f, 0.0f, 0.7f, 0.8f)
        })
        g.draw(rect)
      })

      val edgesToDrawTime = {
        val s = timeline.edgeTimeRange(starts, ends)
        s.sliding(1, s.size / maxEdges max 1).map(_.head).toArray
      }
      val edgesToDrawEnds = {
        val s = if (renderEverything) timeline.edges else timeline.edgeRange(starts, ends)
        s.sliding(1, s.size / maxEdges max 1).map(_.head).toArray
      }

      // Edges
      if (showEdges) {
        g.setStroke(new BasicStroke((2 / zoom).toFloat))
        g.setPaint(new Color(0.5f, 0f, 0.5f, 0.2f))
        (edgesToDrawTime ++ edgesToDrawEnds).foreach(e => {
          val source = timeline.nodesById(e.source)
          val target = timeline.nodesById(e.target)
          boundingBox.add(source.topStartAnchor)
          boundingBox.add(target.startAnchor)
          g.draw(new Line2D.Double(source.topStartAnchor, target.startAnchor))
        })
      }
      
      if (showEdgeMarks) {
        g.setPaint(new Color(0.7f, 0.4f, 0.7f, 0.8f))
        var drawn = 0
        edgesToDrawTime.foreach(e => {
          val source = timeline.nodesById(e.source)
          val target = timeline.nodesById(e.target)
          val t = e.time.toDoubleSeconds
          val r = threadHeight*0.05
          
          if ((t - source.start.toDoubleSeconds).abs > r*2 && (t - target.start.toDoubleSeconds).abs > r*2) {
            val proportion = (t - source.start.toDoubleSeconds) / (target.start.toDoubleSeconds - source.start.toDoubleSeconds)
            
            val y1 = source.topStartAnchor.getY
            val y2 = target.startAnchor.getY
            
            val x = t
            val y = y1 + (y2 - y1) * proportion
            
            g.fill(new Ellipse2D.Double(x-r/2, y-r/2, r, r))
            drawn += 1
          }
        })
        //println(s"Edge marks: ${edgesToDrawTime.size} tried, ${drawn} drawn")
      }

      // Labels
      nodesToDraw.foreach(n => {
        val rect = n.rect
        if (showItems && n.label.length > 0) {
          val t = g.getTransform
          g.translate(rect.getX.toFloat, (rect.getY + rect.getHeight*0.9).toFloat)
          g.rotate(-math.Pi/2)
          g.setPaint(new Color(0.5f, 0.5f, 0.0f))
          g.setFont(itemFont)
          g.drawString(n.label, 0, 0)
          g.setTransform(t)
        }
        val in = timeline.inDegree(n)
        if (showFansIns && in > 1) {
          val t = g.getTransform
          g.translate(rect.getX.toFloat - symbolFont.getSize2D/2, (rect.getY + rect.getHeight).toFloat)
          g.setPaint(new Color(0.9f, 0.0f, 0.0f, 0.5f))
          g.setFont(symbolFont)
          g.drawString(">", 0, 0)
          g.translate(0, -rect.getHeight.toFloat / 2)
          g.setPaint(new Color(0.9f, 0.6f, 0.6f, 0.8f))
          g.setFont(countFont)
          g.drawString(in.toString, 0, 0)
          g.setTransform(t)
        }
        val out = timeline.outDegree(n)
        if (showFansOuts && out > 1) {
          val t = g.getTransform
          g.translate(rect.getX.toFloat, (rect.getY + rect.getHeight).toFloat)
          g.setPaint(new Color(0.0f, 0.9f, 0.0f, 0.5f))
          g.setFont(symbolFont)
          g.drawString("<", 0, 0)
          g.translate(0, -rect.getHeight.toFloat / 2)
          g.setPaint(new Color(0.6f, 0.9f, 0.6f, 0.8f))
          g.setFont(countFont)
          g.drawString(out.toString, 0, 0)
          g.setTransform(t)
        }
      })
      

      //val minPoint = g.getTransform.transform(new Point2D.Double(boundingBox.getX, boundingBox.getY), null)
      //val maxPoint = g.getTransform.transform(new Point2D.Double(boundingBox.getX + boundingBox.getWidth, boundingBox.getY + boundingBox.getHeight), null)
      val boundingDim = new Dimension((boundingBox.getWidth * zoom + 1).toInt, (boundingBox.getHeight * zoom + 1).toInt)
      
      if (getPreferredSize != boundingDim) {
        setPreferredSize(boundingDim)
        //println(s"setPreferredSize: $zoom, $boundingBox, $boundingDim")
        revalidate()
      }
      
      //g.setTransform(outsideTransform)

      {
        //val g = getParent.getParent.getGraphics.asInstanceOf[Graphics2D]
        val textFont = new Font("Arial Bold", 0, 25)
        
        g.setPaint(Color.WHITE)
        g.setFont(textFont)
        val str = (
          (if (showItems) "items, " else "") +
          (if (showFansIns) "fan in, " else "") +
          (if (showFansOuts) "fan out, " else "") +
          (if (showEdges) "edges, " else "") +
          (if (showEdgeMarks) "edge marks, " else "") +
          (if (fastMode) "fast, " else "") +
          (if (renderEverything) "everything, " else "")
          )
        g.drawString(str, 0.0f, getHeight - 25)
        println(str)
      }
      
      //println(s"Rendered: ${nodesToDraw.size} nodes, ${edgesToDrawTime.size + edgesToDrawEnds.size} edges")
      renderEverything = false
    }
  }

  def main(args: Array[String]): Unit = {
    val Array(inputFilename) = args
    val timeline = loadTimeline(inputFilename)
    
    val frame = new JFrame("Timeline")
    val display = new TimelineDisplay(timeline)
    val pane = new JScrollPane(display)
    pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
    pane.getHorizontalScrollBar.setUnitIncrement(20)
    
        
    display.addMouseWheelListener((e: MouseWheelEvent) => {
      if (e.isControlDown) {
        val d = e.getPreciseWheelRotation / 10
        val center = (pane.getHorizontalScrollBar.getValue + display.getWidth/2) / display.zoom
        display.zoom *= (if (d > 0) {
          1.0 / (1 + d)
        } else {
          (1 - d)
        })
        display.repaint()
        SwingUtilities.invokeLater(() =>
          pane.getHorizontalScrollBar.setValue((center * display.zoom).toInt - display.getWidth/2)
          )
      } else {
        pane.dispatchEvent(e)
      }
    })

    frame.addKeyListener(new KeyListener() {
      def keyTyped(e: KeyEvent): Unit = {
        e.getKeyChar match {
          case 'r' => 
            pane.repaint()
          case 'R' =>
            display.renderEverything = true
            pane.repaint()
          case '1' =>
            display.showItems = !display.showItems 
            pane.repaint()
          case '2' =>
            display.showFansIns = !display.showFansIns 
            pane.repaint()
          case '3' =>
            display.showFansOuts = !display.showFansOuts 
            pane.repaint()
          case '4' | 'e' =>
            display.showEdges = !display.showEdges
            pane.repaint()
          case '5' =>
            display.showEdgeMarks = !display.showEdgeMarks
            pane.repaint()
          case 'f' =>
            display.fastMode = !display.fastMode
            pane.repaint()
          case 'q' => 
            frame.setVisible(false)
            System.exit(0)
          case _ => ()
        }
      }
      def keyPressed(e: KeyEvent): Unit = {
      }
      def keyReleased(e: KeyEvent): Unit = {
      }
    })
    frame.setContentPane(pane)
    //frame.setPreferredSize(new Dimension(800, 600))
    frame.setSize(800, 600)
    frame.setVisible(true)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  }
}

