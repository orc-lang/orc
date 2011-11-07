//
// DeleteNode.scala -- Scala class DeleteNode
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
import orc.run.extensions.SwappableASTs
import orc.run.Orc

/**
 * Edit operation that is a deletion of a node.
 *
 * @author jthywiss
 */
case class DeleteNode[A <: AST](deletedNode: A, oldParent: A) extends AstEditOperation {

  def tokenCracker(token: Orc#Token): SwappableASTs#Token = token.asInstanceOf[SwappableASTs#Token]

  def isTokenAffected(token: Orc#Token): Boolean = {
    /* Token at deleted node */
    (tokenCracker(token).node == deletedNode) ||
    /* Token has deleted node in a closure */
    checkClosures(tokenCracker(token).env, Nil) ||
    /* Token has deleted node on its stack */
    checkFrameStack(token)
  }
  
  private def checkClosures(bindings: List[SwappableASTs#Binding], completedClosures: List[SwappableASTs#Closure]): Boolean = {
    for (binding <- bindings)
      binding match {
        case bv: SwappableASTs#BoundValue => {
          bv.v match {
            case c: SwappableASTs#Closure => {
              for (d <- c.defs) if (d == deletedNode) return true
              // Filter recursive bindings
              val cEnv = c.lexicalContext filterNot {
                  case bv2: SwappableASTs#BoundValue => {
                    bv2.v match {
                      case c2: SwappableASTs#Closure => completedClosures.contains(c2)
                    }
                  }
                  case _ => false
              }
              if (checkClosures(cEnv, /*c ::*/ completedClosures)) return true
            }
            case _ => { }
          }
        }
        case _ => { }
      }
    return false
  }

  private def checkFrameStack(token: Orc#Token): Boolean = {
    for (frame <- tokenCracker(token).stack) {
      frame match {
        case sf: SwappableASTs#SequenceFrame if (sf.node == deletedNode) => return true
        case ff: SwappableASTs#FunctionFrame if (ff.callpoint == deletedNode) => return true
        case _ => { }
      }
    }
    return false
  }

  def isTokenSafe(token: Orc#Token): Boolean = { true }//!isTokenAffected(token) }

  def migrateToken(token: Orc#Token): Boolean = {
    if (tokenCracker(token).node == deletedNode) {
      Console.err.println(">>Delete " + token + " at " + deletedNode)
      tokenCracker(token).kill
      true
    } else {
      false
    }
  }

  def migrateClosures(token: Orc#Token) { /*FIXME*/ }

  def migrateFrameStack(token: Orc#Token) { /*FIXME*/ }

}
