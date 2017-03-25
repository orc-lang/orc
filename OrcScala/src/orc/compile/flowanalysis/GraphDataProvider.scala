package orc.compile.flowanalysis

import scala.collection.mutable
import scala.reflect.ClassTag
import orc.util.DotUtils._
import orc.compile.Logger

trait EdgeBase[Node] {
  def from: Node
  def to: Node
}

trait GraphDataProvider[Node, Edge <: EdgeBase[Node]] {
  def nodes: collection.Set[Node]
  def edges: collection.Set[Edge]

  def subgraphs: collection.Set[_ <: GraphDataProvider[Node, Edge]]

  protected[this] lazy val edgeFromIndex: collection.Map[Node, Set[Edge]] = {
    val m = mutable.HashMap[Node, Set[Edge]]()
    for (e <- edges) {
      m += (e.from -> (m.getOrElse(e.from, Set[Edge]()) + e))
    }
    m
  }
  protected[this] lazy val edgeToIndex: collection.Map[Node, Set[Edge]] = {
    val m = mutable.HashMap[Node, Set[Edge]]()
    for (e <- edges) {
      m += (e.to -> (m.getOrElse(e.to, Set[Edge]()) + e))
    }
    m
  }

  def nodesOf[T <: Node: ClassTag] = {
    val TType = implicitly[ClassTag[T]]
    nodes.collect { case TType(e) => e }.toSet
  }

  def nodesBy[U](f: PartialFunction[Node, U]) = {
    nodes.collect { case e if f.isDefinedAt(e) => f(e) }.toSet
  }

  implicit class NodeAdds(n: Node) {
    def outEdges = edgeFromIndex.getOrElse(n, Set())
    def inEdges = edgeToIndex.getOrElse(n, Set())
    def outEdgesOf[T <: Edge: ClassTag] = {
      val TType = implicitly[ClassTag[T]]
      edgeFromIndex.getOrElse(n, Set()).collect { case TType(e) => e }
    }
    def inEdgesOf[T <: Edge: ClassTag] = {
      val TType = implicitly[ClassTag[T]]
      edgeToIndex.getOrElse(n, Set()).collect { case TType(e) => e }
    }
  }

}

trait DebuggableGraphDataProvider[Node <: WithDotAttributes, Edge <: WithDotAttributes with EdgeBase[Node]] extends GraphDataProvider[Node, Edge] {
  def subgraphs: collection.Set[_ <: DebuggableGraphDataProvider[Node, Edge]]

  def graphLabel: String = ""

  def computedNodeDotAttributes(n: Node): DotAttributes = Map()
  def computedEdgeDotAttributes(n: Edge): DotAttributes = Map()

  def renderedNodes: collection.Set[Node] = nodes
  def renderedEdges: collection.Set[Edge] = edges

  def toDot(declarationType: String = "digraph", idMap: mutable.HashMap[AnyRef, String] = mutable.HashMap[AnyRef, String]()): String = {
    def idFor(s: String, o: AnyRef) = idMap.getOrElseUpdate(o, {
      val nextID = idMap.size
      s"$s$nextID"
    })

    val nodesStr = {
      renderedNodes.map { n =>
        val additionalAttr = new WithDotAttributes {
          def dotAttributes = {
            n.dotAttributes ++ computedNodeDotAttributes(n)
          }
        }

        s"""${idFor("n", n)} ${additionalAttr.dotAttributeString};"""
      }.mkString("\n")
    }

    val edgesStr = {
      renderedEdges.map(e => {
        val additionalAttr = new WithDotAttributes {
          def dotAttributes = {
            e.dotAttributes ++ computedEdgeDotAttributes(e)
          }
        }
        s"""${idFor("n", e.from)} -> ${idFor("n", e.to)} ${additionalAttr.dotAttributeString};"""
      }).mkString("\n")
    }

    s"""
${declarationType} ${idFor("cluster", this)} {
compound=true;
label="${quote(graphLabel)}";
${subgraphs.map(_.toDot("subgraph", idMap)).mkString("\n\n")}
$nodesStr
$edgesStr
}
"""
  }

  def debugShow(): Unit = {
    import java.io.File
    import java.nio.charset.StandardCharsets
    import java.nio.file.Files
    import java.nio.file.Paths
    import scala.sys.process._
    val tmpDot = File.createTempFile("orcprog", ".gv");
    val outformat = "svg"
    val tmpPdf = File.createTempFile("orcprog", s".$outformat");
    //tmp.deleteOnExit();
    Logger.info(s"Wrote gz to $tmpDot")
    Logger.info(s"Wrote rendered to $tmpPdf")

    Files.write(Paths.get(tmpDot.toURI), toDot().getBytes(StandardCharsets.UTF_8))
    Seq("dot", s"-T$outformat", tmpDot.getAbsolutePath, s"-o${tmpPdf.getAbsolutePath}").!
    Seq("chromium-browser", tmpPdf.getAbsolutePath).!
  }
}
