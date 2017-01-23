//
// Graph.scala -- Scala class Graph, object Direction, and class Node
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

/** @author srosario
  */
class Graph[T](var nodes: List[Node[T]]) {

  def addEdge(from: Node[T], to: Node[T]) {
    from.succs = to :: from.succs
    to.precs = from :: to.precs
  }

  def depthSearch(dir: Direction.Value): Seq[List[Node[T]]] = {
    var time = 0

    def search(node: Node[T], tree: List[Node[T]]): List[Node[T]] = {
      var resultTree = node :: tree
      time = time + 1
      node.startTime = Some(time)
      var nextNodes = if (dir == Direction.Forward) { node.succs } else { node.precs }
      for (next <- nextNodes) {
        next.startTime match {
          case None => { resultTree = search(next, resultTree) }
          case Some(i) => {}
        }
      }
      time = time + 1
      node.finishTime = Some(time)
      resultTree
    }

    clear
    var forest: Seq[List[Node[T]]] = Seq.empty
    for (n <- nodes) {
      n.startTime match {
        case None => { forest = search(n, Nil) +: forest }
        case Some(i) => {}
      }
    }
    forest
  }

  def sort {
    nodes = nodes sortWith { (n1: Node[T], n2: Node[T]) =>
      (n1.finishTime, n2.finishTime) match {
        case (Some(i), Some(j)) => i > j
        case _ => false // !! Shd not occur.
      }
    }
  }

  def clear {
    for (n <- nodes) {
      n.startTime = None
      n.finishTime = None
    }
  }
}

object Direction extends Enumeration {
  type Direction = Value
  val Forward, Backward = Value
}

class Node[T](val elem: T) {
  var startTime: Option[Int] = None // Start time of the DFS for this node
  var finishTime: Option[Int] = None // End time of the DFS for this node
  var succs: List[Node[T]] = Nil
  var precs: List[Node[T]] = Nil
}
