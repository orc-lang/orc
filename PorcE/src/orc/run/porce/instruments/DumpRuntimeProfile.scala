//
// DumpRuntimeProfile.scala -- Scala object DumpRuntimeProfile
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments

import java.io.PrintWriter

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import scala.collection.mutable

import orc.run.porce.{ CalledRootsProfile, PorcERootNode, SpecializationConfiguration }

import com.oracle.truffle.api.dsl.Introspection
import com.oracle.truffle.api.nodes.Node

object DumpRuntimeProfile {
  import orc.util.DotUtils._

  val metricPrefixMap = SortedMap(
    1.0e18 -> "E",
    1.0e15 -> "P",
    1.0e12 -> "T",
    1.0e9 -> "G",
    1.0e6 -> "M",
    1.0e3 -> "k",
    1.0 -> "",
    1.0e-3 -> "m",
    1.0e-6 -> "µ",
    1.0e-9 -> "n",
    1.0e-12 -> "p",
    1.0e-15 -> "f",
    1.0e-18 -> "a",
  )
  val metricPrefixSearchMap = metricPrefixMap.map({ case (m, p) => (m * 999, (m, p))}) +
        (0.0 -> metricPrefixMap.head) + (Double.MaxValue -> metricPrefixMap.last)

  implicit class NumberAdds(v: Double) {
    def unit(unit: String): String = {
      v match {
        case Long.MaxValue | Long.MinValue => s"? $unit"
        case 0 => s"0 $unit"
        case _ if v < 0 => "-" + (-v unit unit)
        case _ if v.isNaN => s"NaN $unit"
        case _ if v.isInfinity => s"∞ $unit"
        case _ =>
          val (mult, prefix) = metricPrefixSearchMap.from(v.toDouble).head._2
          f"${v.toDouble / mult}%3.1f $prefix$unit"
      }
    }
  }

  def apply(nodes: Iterable[Node], callsRequired: Int, out: PrintWriter): Boolean = {
    if (!SpecializationConfiguration.ProfileCallGraph) {
      return false
    }

    val idMap = mutable.HashMap[AnyRef, String]()
    def idFor(s: String, o: AnyRef) = idMap.getOrElseUpdate(o, {
      val nextID = idMap.size
      s"$s$nextID"
    })

    out.println("digraph G {")
    val methods = nodes.collect({ case r: PorcERootNode => r.getMethodKey }).toSet
    val displayedNodes = collection.mutable.Set[PorcERootNode]()
    val inEdgeCounts = collection.mutable.Map[PorcERootNode, Int]().withDefaultValue(0)
    var totalCallEdgeUses = 0L
    var totalCallEdges = 0L

    def callEdges = nodes.flatMap {
        case r: PorcERootNode =>
          val edges = CalledRootsProfile.getAllCalledRoots(r).asScala
          for (p <- edges if !p.getLeft.isScheduled()) yield p
        case _ => Seq()
    }

    for (p <- callEdges) {
      val to = p.getRight
      inEdgeCounts(to) = inEdgeCounts(to) + 1
      totalCallEdgeUses += p.getLeft.getTotalCalls
      totalCallEdges += 1
    }

    val meanCallEdgeUses = totalCallEdgeUses.toDouble / totalCallEdges

    /* Render a node to the output dot if it has not yet been rendered.
     */
    def displayNode(r: PorcERootNode): Unit = {
      if (displayedNodes contains r)
        return
      displayedNodes += r
      val timeLine = if (r.getTotalTime > 0)
        ((r.getTotalTime.toDouble / r.getTotalCalls / 1000000000.0) unit "s") + "\n"
      else
        ""
      val attrs: Map[String, Any] = Map(
          "label" -> s"""
          |${r.getName}
          |$timeLine${(r.getSiteCalls.toDouble / r.getTotalCalls) unit " site calls"}
          |${r.getTotalCalls unit " calls"}
            """.stripMargin.trim,
          "penwidth" -> (if (r.getTotalTime > 0)
            0.1+10*math.log(1+r.getTotalTime.toDouble / r.getTotalCalls / 1000.0) max 0.1 min 20
          else 1),
          "color" -> (if (inEdgeCounts(r) > 1) "firebrick" else "black"),
          )
      out.println(s"    ${idFor("n", r)} ${attrs.dotAttributeString};")
    }

    for ((m, i) <- methods.zipWithIndex) {
      out.println(s"""  subgraph cluster_$i {\n    label = "${quote(m.toString)}"; """)
      for (n <- nodes) {
        n match {
          case r: PorcERootNode if r.getTotalCalls > 0 && r.getMethodKey == m =>
            displayNode(r)
          case _ => ()
        }
      }
      out.println(s"""  }""")
    }
    for (n <- nodes) {
      n match {
        case root: PorcERootNode => //if r.getTotalCalls > 0 =>
          val edges = CalledRootsProfile.getAllCalledRoots(root).asScala
          for (p <- edges) {
            val callnode = p.getLeft
            val from = callnode.getProfilingScope.asInstanceOf[PorcERootNode]
            val to = p.getRight
            if (to.getTotalCalls > 0) {
              displayNode(from)
              displayNode(to)

              val specNames = try {
                val specs = Introspection.getSpecializations(callnode.asInstanceOf[Node]).asScala
                specs.filter(_.isActive()).map(_.getMethodName())
              } catch {
                case _: IllegalArgumentException => List("<not introspectable>")
              }

              val proportion = callnode.getTotalCalls.toDouble / from.getTotalCalls
              val attrs: Map[String, Any] = Map(
                "label" -> f"""
                |${specNames.mkString("; ")}
                |${callnode.getClass.getSimpleName.toString.take(16)}
                |@${callnode.##}%08x
                |* ${callnode.getTotalCalls unit ""}
                  """.stripMargin.trim,
                "fontsize" -> 8,
                "penwidth" -> (10*math.log(1 + proportion) max 0.1 min 2),
                "color" -> (if (callnode.isScheduled()) "blue" else "black"),
                "style" -> (specNames.head match {
                  case "specificInlineAST" => "dashed"
                  case "selfTail" | "inlinedTail" => "dotted"
                  case _ => "solid"
                }),
                "weight" -> (specNames.head match {
                  case "specificInlineAST" => 100
                  case _ => 1
                })
                )

              out.println(s"  ${idFor("n", from)} -> ${idFor("n", to)} ${attrs.dotAttributeString};")
            }
          }
        case _ => ()
      }
    }
    /*
    val txt = inEdgeCounts.toSeq.sortBy(_._1.getTotalCalls).map({ case (n, ins) =>
      val outs = CalledRootsProfile.getAllCalledRoots(n).asScala.count(!_.getLeft.isScheduled())
      if (ins > 1 || outs > 1)
        s"$n*${n.getTotalCalls unit " calls"} ins=$ins outs=$outs\\l"
      else
        ""
    }).mkString("")
    */
    val targetSpawnCount = totalCallEdgeUses / 100
    val orderedEdges = callEdges.toSeq.sortBy(_.getLeft.getTotalCalls)
    val prefixLen = orderedEdges.scanLeft(0L)(_ + _.getLeft.getTotalCalls).indexWhere(_ > targetSpawnCount)

    val txt = (for (p <- orderedEdges.take(prefixLen + 10)) yield {
      val callnode = p.getLeft
      val from = callnode.getProfilingScope.asInstanceOf[PorcERootNode]
      val to = p.getRight
      s"""(\\"${from.getName}\\", \\"${to.getName}\\") -> SPAWN, // ${callnode.getTotalCalls unit " calls"}\\l"""
    }).mkString("")
    println(s"new CallKindDecision.Table(\n${txt.replace("\\l", "\n").replace("\\\"", "\"")})")

    out.println(s"""  label = "total ${totalCallEdgeUses unit " uses"}, mean ${meanCallEdgeUses unit " uses"}, target ${targetSpawnCount unit " uses"}\\l$txt";\n  labelloc = "t";""")
    out.println("}")
    displayedNodes.nonEmpty
  }
}
