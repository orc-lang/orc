//
// DumpSpecializations.scala -- Scala object DumpSpecializations
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments

import java.io.{ PrintWriter, StringWriter }

import scala.collection.JavaConverters._

import orc.run.porce.{ CalledRootsProfile, HasCalledRoots, HasPorcNode, PorcERootNode }

import com.oracle.truffle.api.dsl.Introspection
import com.oracle.truffle.api.nodes.{ Node, NodeVisitor }

object DumpSpecializations {
  def formatNumWOMax(v: Long): String = {
    if (v == Long.MaxValue) {
      "(n/a)"
    } else {
      v.toString
    }
  }

  def apply(node: Node, callsRequired: Int, out: PrintWriter): Unit = {
    val calledEnough = node match {
      case r: PorcERootNode if (r.getTotalCalls max r.getTotalSpawnedCalls) >= callsRequired =>
        out.println(s"=== ${node}:")
        out.println("    " +
          f"timePerCall (spawned) = ${formatNumWOMax(r.getTimePerCall)}ns " +
          f"(${r.getTotalSpawnedTime.toDouble / r.getTotalSpawnedCalls}%.1f = ${r.getTotalSpawnedTime}/${r.getTotalSpawnedCalls}), " +
          f"timePerCall (all) = ${r.getTotalTime.toDouble / r.getTotalCalls}%.1fns (${r.getTotalTime}/${r.getTotalCalls}), " +
          f"siteCalls = ${r.getSiteCalls.toDouble / r.getTotalCalls}%.1f (${r.getSiteCalls}/${r.getTotalCalls})")
        out.println(s"    All Called Roots = {${CalledRootsProfile.getAllCalledRoots(r).asScala.map(p => {
          val n = p.getLeft
          val r = p.getRight
          s"<${n}*${n.getTotalCalls} -> $r>"
        }).mkString(", ")}}")
        true
      case _ => false
    }
    if (calledEnough) {
      def doVisiting(node: Node): Unit =
        node.accept(new NodeVisitor {
          def visit(node: Node): Boolean = {
            node match {
              /*case l: LoopNode =>
                // Skip loop nodes because they actually have two children which duplicate the body of the loop.
                val children = l.getChildren.asScala
                doVisiting(if (children.size == 1) children.head else children.find(_ != l.getRepeatingNode).getOrElse(children.head))
                false*/
              case _ =>
                printSpecializations(node, out, true, "  ")
                true
            }
          }
        })
      doVisiting(node)
    } else {
      //      out.println(s"   Omitted due to not enough calls.")
    }
  }

  private def printSpecializations(node: Node, out: PrintWriter, doPrintHeader: Boolean, prefix: String) = {
    def printHeader(specsStr: String) = {
      if (doPrintHeader) {
        out.print("--- ")
        findNodeWithSource(node) match {
          case p: HasPorcNode if p.porcNode.isDefined && p.porcNode.get.value.sourceTextRange.isDefined =>
            out.println(s"$prefix${node.getClass} $specsStr\n${p.porcNode.get.value.sourceTextRange.get.lineContentWithCaret}")
          case p: HasPorcNode if p.porcNode.isDefined =>
            out.println(s"$prefix${node.getClass} $specsStr\n${p.porcNode.get.value.prettyprintWithoutNested()}")
          case _ if node.getSourceSection != null =>
            val ss = node.getSourceSection
            out.println(s"$prefix${node.getClass} ${ss.getSource.getName}:${ss.getStartLine} (${ss}) $specsStr")
          case _ =>
            out.println(s"$prefix${node.getClass} $specsStr")
        }
        node match {
          case d: HasCalledRoots if !d.getAllCalledRoots.isEmpty =>
            out.println(s"${prefix}Called Roots = {${d.getAllCalledRoots.asScala.mkString(", ")}}")
          case _ => ()
        }
        out.println(s"$prefix$prefix| ${node}")
      }
    }

    if (Introspection.isIntrospectable(node)) {
      val specs = Introspection.getSpecializations(node).asScala
      val specsStr = specs.filter(_.isActive()).map(_.getMethodName()).mkString("; ")
      printHeader(specsStr)
      for (s <- specs) {
        val n = s.getInstances()
        if (n > 0) {
          out.println(s"$prefix$prefix${s.getMethodName()}: (${n}) ${(0 until n).map(s.getCachedData(_).asScala.mkString("{", ", ", "}")).mkString("; ")}")
        } else if (s.isExcluded()) {
          out.println(s"$prefix$prefix${s.getMethodName()}: EXCLUDED")
        }
      }
    } else if (node.getClass.getAnnotationsByType(classOf[AdhocIntrospectable]).length > 0) {
      printHeader("")
    }

  }

  def specializationsAsString(n: Node): String = {
    val strWriter = new StringWriter()
    printSpecializations(n, new PrintWriter(strWriter), false, "")
    strWriter.toString()
  }

  def findNodeWithSource(node: Node): Node = {
    node match {
      case p: HasPorcNode if p.porcNode.isDefined =>
        node
      case _ if node.getSourceSection != null =>
        node
      case _ if node.getParent != null =>
        findNodeWithSource(node.getParent)
      case _ =>
        node
    }
  }
}
