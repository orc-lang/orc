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

import scala.collection.JavaConverters.asScalaBufferConverter

import orc.run.porce.HasPorcNode

import com.oracle.truffle.api.dsl.Introspection
import com.oracle.truffle.api.nodes.{ Node, NodeVisitor }
import orc.run.porce.PorcERootNode
import com.oracle.truffle.api.dsl.Introspectable

object DumpSpecializations {
  def apply(node: Node, out: PrintWriter): Unit = {
    out.println(s"=== ${node}:")
    node match {
      case r: PorcERootNode =>
        out.println(s"    timePerCall = ${r.getTimePerCall}, totalSpawnedCalls = ${r.getTotalSpawnedCalls}, totalSpawnedTime = ${r.getTotalSpawnedTime}")
      case _ => ()
    }
    node.accept(new NodeVisitor {
      def visit(node: Node): Boolean = {
        printSpecializations(node, out, true, "  ")
        true
      }
    })
  }

  private def printSpecializations(node: Node, out: PrintWriter, doPrintHeader: Boolean, prefix: String) = {
    def printHeader(specsStr: String) = {
      if (doPrintHeader) {
        out.print("--- ")
        findNodeWithSource(node) match {
          case p: HasPorcNode if p.porcNode.isDefined && p.porcNode.get.sourceTextRange.isDefined =>
            out.println(s"$prefix${node.getClass} $specsStr\n${p.porcNode.get.sourceTextRange.get.lineContentWithCaret}")
          case p: HasPorcNode if p.porcNode.isDefined =>
            out.println(s"$prefix${node.getClass} $specsStr\n${p.porcNode.get}")
          case _ if node.getSourceSection != null =>
            val ss = node.getSourceSection
            out.println(s"$prefix${node.getClass} ${ss.getSource.getName}:${ss.getStartLine} (${ss}) $specsStr")
          case _ =>
            out.println(s"$prefix${node.getClass} $specsStr")
        }
        out.println(s"$prefix$prefix| $node")
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
