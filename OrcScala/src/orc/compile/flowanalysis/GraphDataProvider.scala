//
// GraphDataProvider.scala -- Scala trait GraphDataProvider and associates
// Project OrcScala
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.flowanalysis

import scala.collection.mutable
import scala.reflect.ClassTag

import orc.compile.Logger
import orc.util.DotUtils.{ DotAttributes, WithDotAttributes, quote }

trait EdgeBase[Node] {
  def from: Node
  def to: Node
}

trait GraphDataProvider[Node, Edge <: EdgeBase[Node]] {
  def nodes: collection.Set[Node]
  def edges: collection.Set[Edge]

  def entry: Node
  def exit: Node

  def subgraphs: collection.Set[_ <: GraphDataProvider[Node, Edge]]

  protected[this] lazy val edgeFromIndex: collection.Map[Node, Set[Edge]] = {
    val m = mutable.HashMap[Node, Set[Edge]]()
    for (e <- edges) {
      m += (e.from -> (m.getOrElse(e.from, Set[Edge]()) + e))
    }
    collection.immutable.HashMap() ++ m
  }

  protected[this] lazy val edgeToIndex: collection.Map[Node, Set[Edge]] = {
    val m = mutable.HashMap[Node, Set[Edge]]()
    for (e <- edges) {
      m += (e.to -> (m.getOrElse(e.to, Set[Edge]()) + e))
    }
    collection.immutable.HashMap() ++ m
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
      edgeFromIndex.
        getOrElse(n, Set()).
        collect {
          case TType(e) => e
        }
    }
    def inEdgesOf[T <: Edge: ClassTag] = {
      val TType = implicitly[ClassTag[T]]
      edgeToIndex.
        getOrElse(n, Set()).
        collect {
          case TType(e) => e
        }
    }
  }
}

class MutableGraphDataProvider[Node, Edge <: EdgeBase[Node]] extends GraphDataProvider[Node, Edge] {
  private[this] val nodeStore = mutable.Set[Node]()
  private[this] val edgeStore = mutable.Set[Edge]()

  def addNode(n: Node) = {
    nodeStore += n

  }

  def addEdge(e: Edge) = {
    edgeStore += e
    edgeFromIndex += (e.from -> (edgeFromIndex.getOrElse(e.from, Set[Edge]()) + e))
    edgeToIndex += (e.to -> (edgeToIndex.getOrElse(e.to, Set[Edge]()) + e))
    addNode(e.to)
    addNode(e.from)
  }

  def nodes: collection.Set[Node] = nodeStore
  def edges: collection.Set[Edge] = edgeStore

  // TODO: Eliminate these from the top level API.
  def entry: Node = ???
  def exit: Node = ???

  // TODO: Eliminate these from the top level API.
  def subgraphs: collection.Set[_ <: GraphDataProvider[Node, Edge]] = Set()

  protected[this] override lazy val edgeFromIndex: mutable.Map[Node, Set[Edge]] = mutable.HashMap[Node, Set[Edge]]()
  protected[this] override lazy val edgeToIndex: mutable.Map[Node, Set[Edge]] = mutable.HashMap[Node, Set[Edge]]()
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
${declarationType} ${idFor("cluster_", this)} {
label="${quote(graphLabel)}";
${subgraphs.map(_.toDot("subgraph", idMap)).mkString("\n\n")}
$nodesStr
$edgesStr
}
"""
  }

  def debugShow(): Unit = {
    import java.nio.charset.StandardCharsets
    import java.nio.file.Files
    import java.nio.file.Paths
    import scala.sys.process._
    val tmpDot = Files.createTempFile("orcprog", ".gv");
    val outformat = "svg"
    val tmpPdf = Files.createTempFile("orcprog", s".$outformat");
    //tmp.deleteOnExit();

    Files.write(Paths.get(tmpDot.toUri), toDot().getBytes(StandardCharsets.UTF_8))
    Logger.info(s"Wrote gv to $tmpDot")

    Seq("dot", s"-T$outformat", tmpDot.toAbsolutePath.toString, s"-o${tmpPdf.toAbsolutePath.toString}").!
    Logger.info(s"Wrote rendered to $tmpPdf")

    Seq("chromium-browser", tmpPdf.toAbsolutePath.toString).!
  }
}
