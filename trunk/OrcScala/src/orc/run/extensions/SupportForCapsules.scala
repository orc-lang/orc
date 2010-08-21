//
// SupportForCapsules.scala -- Scala class/trait/object SupportForCapsules
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.ast.oil.nameless.Expression
import orc.error.runtime.ExecutionException

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForCapsules extends Orc {
  
  @throws(classOf[ExecutionException])
  def runEncapsulated(node: Expression, caller: Token) = {
    val host = caller.group.root
    val exec = new CapsuleExecution(caller, host)
    val t = new Token(node, exec)
    schedule(t)
  }
  
  class CapsuleExecution(caller: Token, host: Group) extends Subgroup(host) {
    
    var listener: Option[Token] = Some(caller)
    
    override def publish(t: Token, v: AnyRef)= synchronized  { 
      listener match {
        case Some(l) => {
          listener = None
          l.publish(v)
        }
        case None => { } 
      }
      t.halt
    }
    
    override def onHalt= synchronized  {
      listener match {
        case Some(l) => {
          listener = None
          l.halt
        }
        case None => { } 
      }
      host.remove(this)
    }
    
  }
  
}