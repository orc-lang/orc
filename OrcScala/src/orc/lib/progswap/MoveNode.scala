//
// MoveNode.scala -- Scala class MoveNode
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
import orc.run.extensions.SwappableASTs
import orc.run.Orc
import orc.run.core.Token

/** Edit operation that is a relocation of a node within a tree.
  *
  * @author jthywiss
  */
case class MoveNode[A <: AST, B <: AST](movingNode: A, movedNode: B, oldParent: A, newParent: B, position: Int) extends AstEditOperation {

  def tokenCracker(token: Token): Token = token.asInstanceOf[Token]

  def isTokenAffected(token: Token): Boolean = { tokenCracker(token).getNode() == movingNode }

  def isTokenSafe(token: Token): Boolean = { true }

  def migrateToken(token: Token): Boolean = { false }

  def migrateClosures(token: Token) {}

  def migrateFrameStack(token: Token) {}

}
