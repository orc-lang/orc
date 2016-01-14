//
// AstEditScript.scala -- Scala class AstEditScript
// Project OrcScala
//
// Created by jthywiss on Sep 30, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.MutableList
import orc.ast.oil.nameless.NamelessAST

/** A sequence of edit operations on an AST.
  *
  * @author jthywiss
  */
class AstEditScript extends ArrayBuffer[AstEditOperation] {

}

/**
  *
  * @author jthywiss
  */
object AstEditScript {

  //  /**
  //   * Create an AstEditScript describing the operations necessary to
  //   * modify <code>oldOilAst</code> into <code>newOilAst</code>.
  //   *
  //   * @param oldOilAst
  //   * @param newOilAst
  //   * @return the computed AstEditScript
  //   */
  //  def computeEditScript(oldOilAst: NamelessAST, newOilAst: NamelessAST): AstEditScript = {
  //      val editScript = new AstEditScript()
  //      r(editScript, oldOilAst, newOilAst)
  //      editScript
  //  }
  //
  //  /**
  //   * Mindless test implementation -- for two OIL ASTs of the same shape,
  //   * traverse both in lock-step and replace each old node with the
  //   * corresponding new node.
  //   */
  //  def r(script: AstEditScript, oldNode: NamelessAST, newNode: NamelessAST): AstEditScript = {
  //    script += new ReplaceNode(oldNode, newNode)
  //    val oldIter = oldNode.subtrees.iterator
  //    val newIter = newNode.subtrees.iterator
  //    while (oldIter.hasNext && newIter.hasNext) {
  //      r(script, oldIter.next(), newIter.next());
  //    }
  //    if (oldIter.hasNext || newIter.hasNext) {
  //      throw new AssertionError("ASTs are not the same shape");
  //    }
  //    script
  //  }

}
