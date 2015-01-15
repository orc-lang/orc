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

import orc.ast.AST
import orc.ast.oil.nameless.{ Def, Expression, NamelessAST }
import orc.run.extensions.SwappableASTs
import orc.run.Orc
import orc.run.core.BoundValue
import orc.run.core.Token
import orc.run.core.Closure
import orc.run.core.Binding
import orc.run.core.FunctionFrame
import orc.run.core.SequenceFrame

/** Edit operation that is a to-one mapping of an AST node.
  *
  * @author jthywiss
  */
case class ReplaceNode[A <: AST, B <: AST](oldNode: A, newNode: B) extends AstEditOperation {
  def tokenCracker(token: Token): Token = token.asInstanceOf[Token]

  def isTokenAffected(token: Token): Boolean = { tokenCracker(token).getNode() == oldNode }

  def isTokenSafe(token: Token): Boolean = { true }

  def migrateToken(token: Token): Boolean = {
    if (isTokenAffected(token)) {
      Console.err.println(">>Move " + token + " from " + oldNode + " to " + newNode)
      tokenCracker(token).move(newNode.asInstanceOf[Expression])
      true
    } else {
      false
    }
  }

  def migrateClosures(token: Token) {
    migrateClosures(tokenCracker(token).getEnv(), Nil)
  }

  private def migrateClosures(bindings: List[Binding], completedClosures: List[Closure]) {
    for (binding <- bindings)
      binding match {
        case bv: BoundValue => {
          bv.v match {
            case c: Closure => {
              SwappableASTs.setClosureDef(c, c.collection.definitions map { d => if (d == oldNode) newNode.asInstanceOf[Def] else d })
              // Filter recursive bindings
              val cEnv = c.lexicalContext filterNot {
                case bv2: BoundValue => {
                  bv2.v match {
                    case c2: Closure => completedClosures.contains(c2)
                  }
                }
                case _ => false
              }
              migrateClosures(cEnv, /*c ::*/ completedClosures)
            }
            case _ => {}
          }
        }
        case _ => {}
      }
  }

  def migrateFrameStack(token: Token) {
    Console.err.println(">>ReplaceNode.migrateFrameStack(" + token + ")")
    for (frame <- tokenCracker(token).getStack()) {
      Console.err.println("  " + frame)
      frame match {
        case sf: SequenceFrame if (sf.node == oldNode) => SwappableASTs.setSequenceFrameNode(sf, newNode.asInstanceOf[Expression])
        case ff: FunctionFrame if (ff.callpoint == oldNode) => SwappableASTs.setFunctionFrameCallpoint(ff, newNode.asInstanceOf[Expression])
        case _ => {}
      }
    }
  }

}
