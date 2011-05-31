//
// FastMatchCRGMW96.scala -- Scala object FastMatchCRGMW96
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

import orc.ast.AST
import scala.collection.mutable.Map
import scala.math.max

/**
 * Given two ASTs, compute a matching, a sequence of pairs (oldNode, newNode)
 * where the two nodes are "equal" nodes in the given ASTs.
 *
 * @author jthywiss
 */
trait MatchAsts {
  def matchAsts(ast1: AST, ast2: AST): Seq[Tuple2[AST, AST]]
}


/**
 * Given two ASTs, compute a matching, a sequence of pairs (oldNode, newNode)
 * where the two nodes are "equal" nodes in the given ASTs.
 *
 * Implemented using the FastMatch algorithm in:
 *
 * Chawathe, S. S., Rajaraman, A., Garcia-Molina, H., and Widom, J. 1996.
 * Change detection in hierarchically structured information. In Proceedings
 * of the 1996 ACM SIGMOD International Conference on Management of Data
 * (Montreal, Quebec, Canada, 04–06 Jun 1996). ACM, 493–504.
 *
 * @author jthywiss
 */
object FastMatchCRGMW96 extends MatchAsts {

  def matchAsts(ast1: AST, ast2: AST): Seq[Tuple2[AST, AST]] = {
    //FIXME: CRGMW96 specifies each of these are done on a node-type by node-type basis.
    //FIXME: CRGMW96 specifies inorder traversal

    val matches = Map[AST, AST]()

    /* Match leaves */
    {
      val leaves1 = traverseLeavesInOrder(ast1, (_:AST).subtrees).toIndexedSeq
      val leaves2 = traverseLeavesInOrder(ast2, (_:AST).subtrees).toIndexedSeq
      matchChains(leaves1, leaves2, {(_:AST).equals(_:AST)}, matches)
    }

    /* Match interior nodes */
    {
      val leafDescendents1 = IdentityHashMap[AST, Seq[AST]]()
      tabulateLeafDescendents(leafDescendents1, ast1)
      val leafDescendents2 = IdentityHashMap[AST, Seq[AST]]()
      tabulateLeafDescendents(leafDescendents2, ast2)
      val interiorNodes1 = traverseInteriorNodesPreOrder(ast1, (_:AST).subtrees).toIndexedSeq
      val interiorNodes2 = traverseInteriorNodesPreOrder(ast2, (_:AST).subtrees).toIndexedSeq
      matchChains(interiorNodes1, interiorNodes2, interiorNodeEqual(_: AST, _: AST, leafDescendents1, leafDescendents2, matches), matches)
    }

    /* Cumulative result, as a sequence of pairs */
    matches.toSeq
  }

  def matchChains[A, B](chain1: Seq[A], chain2: Seq[B], isEqual: (A, B) => Boolean, matches: Map[A, B]) {
    val lcs = LCSMyers86.lcs(chain1, chain2, isEqual)
    matches ++= lcs._1 zip lcs._2

    /*  Match chain nodes not in the LCS */
    val remain2 = chain2.diff(lcs._2).toBuffer
    for (node1 <- chain1.diff(lcs._1)) {
      val i = remain2.indexWhere(isEqual(node1, _))
      if (i >= 0) {
        val node2 = remain2(i)
        remain2.remove(i)
        matches += ((node1, node2))
      }
    }
  }
  
  def tabulateLeafDescendents(table: Map[AST, Seq[AST]], node: AST): Iterable[AST] = {
    if (node == null || node.subtrees.isEmpty) {
      List(node)
    } else {
      val leafDescendents = node.subtrees flatMap { tabulateLeafDescendents(table, _: AST) }
      table += ((node, leafDescendents.toSeq))
      leafDescendents
    }
  }

  def interiorNodeEqual(node1: AST, node2: AST, leafDescendants1: PartialFunction[AST, Seq[AST]], leafDescendants2: PartialFunction[AST, Seq[AST]], matchOf1: PartialFunction[AST, AST]): Boolean = {
    if (node1.getClass != node2.getClass) {
      return false
    } else {
      var remain = (max(leafDescendants1(node1).size, leafDescendants2(node2).size) * 0.4 /* param 't' */).toInt
      for (d1 <- leafDescendants1(node1)) {
        if (matchOf1.isDefinedAt(d1) && leafDescendants2(node2).contains(matchOf1(d1))) { remain -= 1 }
        if (remain < 0) {
          return true
        }
      }
      return false
    }
  }

  //TODO: Rewrite these as lazy.
  def traverseLeavesInOrder[A](node: A, childMap: A => Traversable[A]): Traversable[A] = {
    val children = childMap(node)
    if (children == null || children.isEmpty) {
      List(node)
    } else {
      children flatMap {traverseLeavesInOrder(_, childMap)}
    }
  }

  def traverseInteriorNodesPreOrder[A](node: A, childMap: A => TraversableOnce[A]): Traversable[A] = {
    val children = childMap(node).toSeq
    if (children != null && !children.isEmpty) {
      node +: (children flatMap {traverseInteriorNodesPreOrder(_, childMap)})
    } else {
      Nil
    }
  }

}
