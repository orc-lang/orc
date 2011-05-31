//
// ReplaceNode.scala -- Scala class ReplaceNode
// Project OrcScala
//
// $Id$
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

import orc.ast.oil.nameless.Def
import scala.collection.mutable.Buffer
import orc.run.extensions.SwappableASTs
import orc.ast.oil.nameless.NamelessAST
import orc.ast.oil.nameless.Expression
import orc.run.Orc

/**
 * Edit operation that is a to-one mapping of an AST node. 
 *
 * @author jthywiss
 */
case class ReplaceNode[A, B](oldNode: A, newNode: B) extends AstEditOperation {
  def tokenCracker(token: Orc#Token): SwappableASTs#Token = token.asInstanceOf[SwappableASTs#Token]

  def isTokenAffected(token: Orc#Token): Boolean = { tokenCracker(token).node == oldNode }

  def isTokenSafe(token: Orc#Token): Boolean = { true }

  def migrateToken(token: Orc#Token): Boolean = {
    if (isTokenAffected(token)) {
      Console.err.println(">>Move " + token + " from " + oldNode + " to " + newNode)
      tokenCracker(token).move(newNode.asInstanceOf[Expression])
      true
    } else {
      false
    }
  }

  def migrateClosures(token: Orc#Token) {
    migrateClosures(tokenCracker(token).env, Nil)
  }

  private def migrateClosures(bindings: List[SwappableASTs#Binding], completedClosures: List[SwappableASTs#Closure]) {
    for (binding <- bindings)
      binding match {
        case bv: SwappableASTs#BoundValue => {
          bv.v match {
            case c: SwappableASTs#Closure => {
              SwappableASTs.setClosureDef(c, c.defs map { d => if (d == oldNode) newNode.asInstanceOf[Def] else d })
              // Filter recursive bindings
              val cEnv = c.lexicalContext filterNot {
                  case bv2: SwappableASTs#BoundValue => {
                    bv2.v match {
                      case c2: SwappableASTs#Closure => completedClosures.contains(c2)
                    }
                  }
                  case _ => false
              }
              migrateClosures(cEnv, /*c ::*/ completedClosures)
            }
            case _ => { }
          }
        }
        case _ => { }
      }
  }

  def migrateFrameStack(token: Orc#Token) {
    for (frame <- tokenCracker(token).stack) {
      frame match {
        case sf: SwappableASTs#SequenceFrame if (sf.node == oldNode) => SwappableASTs.setSequenceFrameNode(sf, newNode.asInstanceOf[Expression])
        case ff: SwappableASTs#FunctionFrame if (ff.callpoint == oldNode) => SwappableASTs.setFunctionFrameCallpoint(ff, newNode.asInstanceOf[Expression])
        case _ => { }
      }
    }
  }

}
