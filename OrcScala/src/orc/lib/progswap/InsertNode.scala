//
// InsertNode.scala -- Scala class InsertNode
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
import orc.run.core.Token

/** Edit operation that is an addition of a new node to a tree.
  *
  * @author jthywiss
  */
case class InsertNode[A <: AST, B <: AST](newNode: B, oldParent: A, newParent: B, position: Int) extends AstEditOperation {

  def isTokenAffected(token: Token): Boolean = { false }

  def isTokenSafe(token: Token): Boolean = { true }

  def migrateToken(token: Token): Boolean = { false }

  def migrateClosures(token: Token) {}

  def migrateFrameStack(token: Token) {}

}
