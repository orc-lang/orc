//
// IndexAST.scala -- Scala object IndexAST
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.porc

import orc.ast.ASTWithIndex
import swivel.Zipper

class IndexAST {
  private var i = 0
  private def nextIndex(j: Int): Int = {
    if (j > i) {
      i = j + 1
      j
    } else {
      val r = i
      i += 1
      r
    }
  }

  private val indexedASTs = collection.mutable.HashSet[Zipper]()

  def apply(ast: PorcAST.Z): Unit = {
    def process(ast: PorcAST.Z): Unit = {
      ast match {
        case a @ Zipper(n: ASTWithIndex, _) if !indexedASTs.contains(a) =>
          val ni = nextIndex(n.optionalIndex.getOrElse(Int.MinValue))
          n.optionalIndex match {
            case Some(oi) if ni != oi =>
              Logger.warning(s"${ast.toString.take(100)} already has index $oi, but being assigned index $ni")
            case _ => ()
          }
          n.optionalIndex = Some(ni)
          indexedASTs += a
        case _ => ()
      }
      ast.subtrees.foreach(process)
    }

    process(ast)
  }
}

object IndexAST {
  def apply(ast: PorcAST.Z): Unit = {
    new IndexAST()(ast)
  }
}
