//
// DeleteNode.scala -- Scala class DeleteNode
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
import orc.run.core.BoundValue
import orc.run.core.Token
import orc.run.core.Closure
import orc.run.core.Binding
import orc.run.core.FunctionFrame
import orc.run.core.SequenceFrame

/** Edit operation that is a deletion of a node.
  *
  * @author jthywiss
  */
case class DeleteNode[A <: AST](deletedNode: A, oldParent: A) extends AstEditOperation {

  def tokenCracker(token: Token): Token = token.asInstanceOf[Token]

  def isTokenAffected(token: Token): Boolean = {
    /* Token at deleted node */
    (tokenCracker(token).getNode() == deletedNode) ||
      /* Token has deleted node in a closure */
      checkClosures(tokenCracker(token).getEnv(), Nil) ||
      /* Token has deleted node on its stack */
      checkFrameStack(token)
  }

  private def checkClosures(bindings: List[Binding], completedClosures: List[Closure]): Boolean = {
    for (binding <- bindings)
      binding match {
        case bv: BoundValue => {
          bv.v match {
            case c: Closure => {
              for (d <- c.collection.definitions) if (d == deletedNode) return true
              // Filter recursive bindings
              val cEnv = c.lexicalContext filterNot {
                case bv2: BoundValue => {
                  bv2.v match {
                    case c2: Closure => completedClosures.contains(c2)
                  }
                }
                case _ => false
              }
              if (checkClosures(cEnv, /*c ::*/ completedClosures)) return true
            }
            case _ => {}
          }
        }
        case _ => {}
      }
    return false
  }

  private def checkFrameStack(token: Token): Boolean = {
    for (frame <- tokenCracker(token).getStack()) {
      frame match {
        case sf: SequenceFrame if (sf.node == deletedNode) => return true
        case ff: FunctionFrame if (ff.callpoint == deletedNode) => return true
        case _ => {}
      }
    }
    return false
  }

  def isTokenSafe(token: Token): Boolean = { true } //!isTokenAffected(token) }

  def migrateToken(token: Token): Boolean = {
    if (tokenCracker(token).getNode() == deletedNode) {
      Console.err.println(">>Delete " + token + " at " + deletedNode)
      tokenCracker(token).kill
      true
    } else {
      false
    }
  }

  def migrateClosures(token: Token) { /*FIXME*/ }

  def migrateFrameStack(token: Token) { /*FIXME*/ }

}
