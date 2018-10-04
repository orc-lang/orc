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

import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.mutable

import orc.run.porce.{ CalledRootsProfile, PorcERootNode }

import com.oracle.truffle.api.nodes.Node
import scala.collection.immutable.SortedMap
import orc.util.DotUtils

object DumpRuntimeProfile {
  import DotUtils.quote

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

  def apply(nodes: Iterable[Node], callsRequired: Int, out: PrintWriter): Unit = {
    val idMap = mutable.HashMap[AnyRef, String]()
    def idFor(s: String, o: AnyRef) = idMap.getOrElseUpdate(o, {
      val nextID = idMap.size
      s"$s$nextID"
    })

    out.println("digraph G {")
    val methods = nodes.collect({ case r: PorcERootNode => r.getMethodKey }).toSet
    for ((m, i) <- methods.zipWithIndex) {
      out.println(s"""  subgraph cluster_$i {\n    label = "${quote(m.toString)}"; """)
      for (n <- nodes) {
        n match {
          case r: PorcERootNode if /*r.getTotalCalls > 0 &&*/ r.getMethodKey == m =>
            out.println(s"    ${idFor("n", r)} [" +
                s"""label="${quote(r.getName)}\n${(r.getTotalTime.toDouble / r.getTotalCalls / 1000000000.0) unit "s"}, """ +
                s"""${(r.getSiteCalls.toDouble / r.getTotalCalls) unit " site calls"}\n${r.getTotalCalls unit " calls"}", """ +
                s"""penwidth=${0.1+10*math.log(1+r.getTotalTime.toDouble / r.getTotalCalls / 1000.0) max 0.1 min 20}];""")
          case _ => ()
        }
      }
      out.println(s"""  }""")
    }
    for (n <- nodes) {
      n match {
        case r: PorcERootNode => //if r.getTotalCalls > 0 =>
          for (p <- CalledRootsProfile.getAllCalledRoots(r).asScala) {
            val node = p.getLeft
            val to = p.getRight

            /*if (to.getTotalCalls > 0)*/ {
              // node.getTotalCalls > r.getTotalCalls/1000 &&
              val proportion = node.getTotalCalls.toDouble / r.getTotalCalls
              out.println(f"  ${idFor("n", r)} -> ${idFor("n", to)} [" +
                f"""label="${quote(node.getClass.getSimpleName.toString.take(16))}\n@${node.##}%08x\n* ${node.getTotalCalls unit ""}", fontsize=8, """ +
                f"""penwidth=${10*math.log(1 + proportion) max 0.1 min 2}];""")
            }
          }
        case _ => ()
      }
    }
    out.println("}")
  }

}
