//
// MoveNode.scala -- Scala class MoveNode
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

import orc.ast.oil.nameless.NamelessAST
import orc.run.extensions.SwappableASTs
import orc.run.Orc

/**
 * Edit operation that is a relocation of a node within a tree.
 *
 * @author jthywiss
 */
case class MoveNode[A, B](movingNode: A, movedNode: B, oldParent: A, newParent: B, position:Int) extends AstEditOperation {

  def tokenCracker(token: Orc#Token): SwappableASTs#Token = token.asInstanceOf[SwappableASTs#Token]

  def isTokenAffected(token: Orc#Token): Boolean = { tokenCracker(token).node == movingNode }

  def isTokenSafe(token: Orc#Token): Boolean = { true }

  def migrateToken(token: Orc#Token): Boolean = { false }

  def migrateClosures(token: Orc#Token): Unit = {  }

  def migrateFrameStack(token: Orc#Token): Unit = {  }

}
