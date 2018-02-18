package orc.run.porce.instruments

import com.oracle.truffle.api.nodes.Node
import java.io.PrintStream
import com.oracle.truffle.api.nodes.NodeVisitor
import orc.run.porce.NodeBase
import com.oracle.truffle.api.dsl.Introspection
import scala.collection.JavaConverters._
import orc.run.porce.HasPorcNode
import java.io.PrintWriter
import java.io.StringWriter

object DumpSpecializations {
  def apply(node: Node, out: PrintWriter): Unit = {
    out.println(s"Specializations for ${node}:")
    node.accept(new NodeVisitor {
      def visit(node: Node): Boolean = {
        printSpecializations(node, out, true, "  ")
        true
      }
    })
  }

  private def printSpecializations(node: Node, out: PrintWriter, printHeader: Boolean, prefix: String) = {
    if (Introspection.isIntrospectable(node)) {
      val specs = Introspection.getSpecializations(node).asScala
      // Using lazy here to make sure this runs only once.
      lazy val header = if (printHeader) {
        val specsStr = specs.filter(_.isActive()).map(_.getMethodName()).mkString("; ")
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
        ()
      }
      for (s <- specs) {
        val n = s.getInstances()
        if (n > 0) {
          header
          out.println(s"$prefix$prefix${s.getMethodName()}: (${n}) ${(0 until n).map(s.getCachedData(_).asScala.mkString("{", ", ", "}")).mkString("; ")}")              
        } else if (s.isExcluded()) {
          header
          out.println(s"$prefix$prefix${s.getMethodName()}: EXCLUDED")              
        }
      }
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