//
// EditScriptCRGMW96.scala -- Scala object EditScriptCRGMW96
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

import orc.ast.AST
import scala.collection.mutable.{ ArrayBuffer, HashMap, Map }
import scala.collection.mutable.MapLike
import scala.collection.generic.MutableMapFactory
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Buffer

/** Create an edit script describing the operations necessary to
  * modify a given AST into another given AST.
  *
  * Implemented using the EditScript algorithm in:
  *
  * Chawathe, S. S., Rajaraman, A., Garcia-Molina, H., and Widom, J. 1996.
  * Change detection in hierarchically structured information. In Proceedings
  * of the 1996 ACM SIGMOD International Conference on Management of Data
  * (Montreal, Quebec, Canada, 04–06 Jun 1996). ACM, 493–504.
  *
  * @author jthywiss
  */
object EditScriptCRGMW96 {
  /** Create an AstEditScript describing the operations necessary to
    * modify <code>oldOilAst</code> into <code>newOilAst</code>.
    *
    * @param oldOilAst
    * @param newOilAst
    * @return the computed AstEditScript
    */
  def computeEditScript[A <: AST, B <: AST](oldOilAst: A, newOilAst: B, matching: Seq[(A, B)]): AstEditScript = {

    /* Old -> new and new -> old partial maps */
    val matchOldNew = IdentityHashMap(matching.toBuffer: _*)
    val matchNewOld = matchOldNew map { case (o, n) => (n, o) }

    val editScript = new AstEditScript()

    /* Memoize a mutable children map and parent map for each AST node */
    def tabulateRelations[N <: AST](parentTable: Map[N, N], childrenTable: Map[N, Buffer[N]], parent: N) {
      val children = parent.subtrees.toBuffer.asInstanceOf[Buffer[N]]
      childrenTable += ((parent, children))
      for (child <- children) {
        parentTable += ((child, parent))
        tabulateRelations(parentTable, childrenTable, child)
      }
    }
    val parentOld = IdentityHashMap[A, A]()
    val childrenOld = IdentityHashMap[A, Buffer[A]]()
    tabulateRelations(parentOld, childrenOld, oldOilAst)
    val parentNew = IdentityHashMap[B, B]()
    val childrenNew = IdentityHashMap[B, Buffer[B]]()
    tabulateRelations(parentNew, childrenNew, newOilAst)

    /* Set of nodes that are known to be in their new position */
    val inOrder = new IdentityHashSet[AST]()

    /* Determine the new location of newNode among its siblings */
    def findPos(newNode: B): Int = {
      val siblingsNew = childrenNew(parentNew(newNode))
      var previousInOrderSibling = siblingsNew.head
      val iter = siblingsNew.view.filter({ inOrder.contains(_) }).iterator
      while (iter.hasNext && (previousInOrderSibling ne newNode)) {
        previousInOrderSibling = iter.next
      }
      if (previousInOrderSibling eq newNode) return 0
      val matchOfPreviousInOrderSibling = matchNewOld(previousInOrderSibling)
      val i = childrenOld(parentOld(matchOfPreviousInOrderSibling)).filter((inOrder.contains(_))).toSeq.indexOf(matchOfPreviousInOrderSibling)
      i + 1
    }

    /* Insert, Update, and Move phase */
    for (newNode <- traverseBreadthFirst(newOilAst, childrenNew)) {
      var oldNode = matchNewOld.get(newNode)
      if (oldNode.isEmpty) {
        val k = findPos(newNode)
        oldNode = Some(newNode.asInstanceOf[A]) // A bit of a hack, but an oldNode is needed
        val matchOfParentNewNode = matchNewOld(parentNew(newNode))
        editScript += new InsertNode(newNode, matchOfParentNewNode, parentNew(newNode), k)
        /* Update old tree to reflect insertion */
        matchOldNew += ((oldNode.get, newNode))
        matchNewOld += ((newNode, oldNode.get))
        parentOld += ((oldNode.get, matchOfParentNewNode))
        childrenOld += ((oldNode.get, ArrayBuffer.empty))
        val siblings = childrenOld(matchOfParentNewNode)
        siblings.insert(k, oldNode.get)
        childrenOld.update(matchOfParentNewNode, siblings)
      } else if (newNode ne newOilAst) {
        val parentOldNode = parentOld(oldNode.get)
        val matchOfParentNewNode = matchNewOld(parentNew(newNode))
        editScript += new ReplaceNode(oldNode.get, newNode)
        if (matchOfParentNewNode != parentOldNode) {
          val k = findPos(newNode)
          editScript += new MoveNode(oldNode.get, newNode, matchOfParentNewNode, parentNew(newNode), k)
          /* Update old tree to reflect move */
          parentOld.update(oldNode.get, matchOfParentNewNode)
          childrenOld.update(parentOldNode, childrenOld(parentOldNode).filterNot(_ eq oldNode.get))
          val siblings = childrenOld(matchOfParentNewNode)
          siblings.insert(k, oldNode.get)
          childrenOld.update(matchOfParentNewNode, siblings)
        }
      } else {
        // The only operation that is available on the tree roots is "replace"
        editScript += new ReplaceNode(oldOilAst, newOilAst)
      }

      /* Align children of old/new nodes */
      val childrenOldNode = childrenOld(oldNode.get)
      val childrenNewNode = childrenNew(newNode)
      val s1 = childrenOldNode.filter({ n: A => matchOldNew.isDefinedAt(n) && childrenNewNode.exists(_ == matchOldNew(n)) }).toIndexedSeq
      val s2 = childrenNewNode.filter({ n: B => matchNewOld.isDefinedAt(n) && childrenOldNode.exists(_ == matchNewOld(n)) }).toIndexedSeq
      val p = LCSMyers86.lcs(s1, s2, { matchOldNew(_: A) eq (_: B) })
      p._1.foreach(inOrder.add(_))
      p._2.foreach(inOrder.add(_))
      for (oldChild <- s1 if !inOrder.contains(oldChild)) {
        val newChild = matchOldNew(oldChild)
        val k = findPos(newChild)
        editScript += new MoveNode(oldChild, newChild, oldNode.get, newNode, k)
        /* Update old tree to reflect move */
        childrenOldNode -= (oldChild)
        childrenOldNode.insert(k, oldChild)
        inOrder.add(oldChild)
        inOrder.add(matchOldNew(oldChild))
      }
      childrenOld.update(oldNode.get, childrenOldNode)
    }

    /* Delete phase */
    for (oldNode <- traversePostOrder(oldOilAst, childrenOld)) {
      if (matchOldNew.get(oldNode).isEmpty) {
        editScript += new DeleteNode(oldNode, parentOld(oldNode))
      }
    }

    /* Assert matchNewOld is total */
    assert((matchNewOld.keys.toSeq diff traversePostOrder(newOilAst, childrenNew).toSeq).isEmpty)

    editScript
  }

  //TODO: Rewrite these as lazy.
  def traversePostOrder[A](node: A, childMap: A => Traversable[A]): Traversable[A] = {
    val children = childMap(node).toSeq
    (children flatMap { traversePostOrder(_, childMap) }) :+ node
  }

  def traverseBreadthFirst[A](root: A, childMap: A => TraversableOnce[A]): Traversable[A] = {
    val q = ArrayBuffer[A]()
    q += root
    /* Note q.iterator, q.forEach, q.map, etc., and for statements
     * don't work when the underlying collection changes. */
    var i = 0
    while (i < q.size) {
      val node = q(i)
      i += 1
      q ++= childMap(node)
    }
    q
  }

}

/** A mutable map from keys of type A to values of type B.
  * Keys are compared for equality using reference equality (<code>AnyRef.eq</code>),
  * in contrast to other Map classes, which use semantic equality (<code>Any.==</code>).
  * Analogously, identity hash codes are used for hashing.
  *
  * @author jthywiss
  */
class IdentityHashMap[A, B] extends HashMap[A, B]
  with MapLike[A, B, IdentityHashMap[A, B]] {
  override def empty: IdentityHashMap[A, B] = new IdentityHashMap[A, B]()
  override protected def elemHashCode(key: A) = System.identityHashCode(key)
  override protected def elemEquals(key1: A, key2: A): Boolean = key1.asInstanceOf[AnyRef] eq key2.asInstanceOf[AnyRef]
  override def par = throw new UnsupportedOperationException("IdentityHashMap.par not implemented")
}

/** Provides provides a set of operations to create IdentityHashMap values.
  *
  * @author jthywiss
  */
object IdentityHashMap extends MutableMapFactory[IdentityHashMap] {
  implicit def canBuildFrom[A, B]: CanBuildFrom[Coll, (A, B), IdentityHashMap[A, B]] = new MapCanBuildFrom[A, B]
  override def empty[A, B]: IdentityHashMap[A, B] = new IdentityHashMap[A, B]()
}

/** A mutable set of values of type A.
  * Values are compared for equality using reference equality (<code>AnyRef.eq</code>),
  * in contrast to other Set classes, which use semantic equality (<code>Any.==</code>).
  * Analogously, identity hash codes are used for hashing.
  *
  * [Minimal implementation, only <code>add</code> and <code>contains</code> methods.]
  *
  * @author jthywiss
  */
class IdentityHashSet[A] {
  val map = IdentityHashMap[A, A]()
  def add(elem: A): Boolean = {
    map.put(elem, elem).isEmpty
  }
  def contains(elem: A): Boolean = map.contains(elem)
}
