//
// SupportForClasses.scala -- Scala trait SupportForClasses
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.ast.oil.nameless.{Expression, Call, Constant}
import orc.{ OrcExecutionOptions, OrcEvent, Handle }
import orc.error.runtime.ExecutionException

/**
 * 
 *
 * @author dkitchin
 */

case class InstanceEvent(c: AnyRef, args: List[AnyRef], caller: Handle) extends OrcEvent

trait SupportForClasses extends Orc { self =>
 
  override def generateOrcHandlers(host: Execution): List[OrcHandler] = {
    val thisHandler = {
      case InstanceEvent(closure, args, caller) => {
        assert(closure.isInstanceOf[self.Closure])
        val node = Call(Constant(closure), args map Constant, Some(Nil))
        val exec = new ClassExecution(caller, host)
        val t = new Token(node, exec)
        schedule(t)
      }
    } : PartialFunction[OrcEvent, Unit]
    
    thisHandler :: super.generateOrcHandlers(host)
  }

  
  class ClassExecution(caller: Handle, host: Group) extends Subgroup(host) {

    var listener: Option[Handle] = Some(caller)

    override def publish(t: Token, v: AnyRef) = synchronized {
      listener match {
        case Some(l) => {
          listener = None
          l.publish(v)
        }
        case None => {}
      }
      t.halt
    }

    override def onHalt = synchronized {
      listener match {
        case Some(l) => {
          listener = None
          l.halt
        }
        case None => {}
      }
      host.remove(this)
    }

  }

}