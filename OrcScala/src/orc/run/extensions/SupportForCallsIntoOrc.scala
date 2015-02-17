//
// SupportForCallsIntoOrc.scala -- Scala trait SupportForCallsIntoOrc
// Project OrcScala
//
// $Id: SupportForSynchronousExecution.scala 3387 2015-01-12 21:57:11Z arthur.peters@gmail.com $
//
// Created by amp on Feb 11, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime
import orc.OrcEvent
import orc.HaltedOrKilledEvent
import orc.OrcExecutionOptions
import orc.ast.oil.nameless.Expression
import orc.error.runtime.ExecutionException
import orc.util.LatchingSignal
import orc.run.Orc
import orc.ast.oil.nameless.Call
import orc.ast.oil.nameless.Constant
import orc.run.core.Token
import orc.run.core.Subgroup
import orc.run.core.Group
import orc.run.core.Binding
import orc.run.core.BoundValue
import orc.run.core.BoundStop
import orc.run.core.BoundValue
import orc.values.Field
import orc.ast.oil.nameless.Variable
import orc.ast.oil.nameless.FieldAccess


/** A group that stores the first publication and then ignores the rest.
  *
  * This is much like a GraftGroup however Futures do not allow real blocking on the values.
  * 
  * @author amp
  */
class OrcCallWrapperGroup(parent: Group) extends Subgroup(parent) {
  import OrcCallWrapperGroup._
  var state: State = Unbound
  
  def bind(b: Option[AnyRef]) = synchronized {
    state match {
      case Unbound => 
        state = Bound(b)
        this.notifyAll()
      case _ => // Just ignore the binding.
    }
  }
  
  /** Wait for the value.
    *  
    * Only one thread may do this as the state is cleared then the value is returned.
    * This is required to maintain an invarient that groups never hold references to values.
    */
  def await() = synchronized {
    assert(state != Completed)
    while(!state.isInstanceOf[Bound]) {
      this.wait()
    }
    state match {
      case Bound(v) => 
        state = Completed
        v
    }
  }
  
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    bind(v)
    t.halt()
  }

  def onHalt() = {
    bind(None)
    parent.remove(this)
  }
  
  def onDiscorporate() = {
    parent.remove(this)
  }
}

object OrcCallWrapperGroup {
  sealed trait State
  
  case object Unbound extends State
  case class Bound(value: Option[AnyRef]) extends State
  case object Completed extends State
}

/** Provide support for calling into Orc from Java code that has access to the OrcRuntime instance.
  * @author amp
  */
trait SupportForCallsIntoOrc extends OrcRuntime {
  this: Orc =>
  
  /** Call the callable with the given arguments and then return the first thing it publishes.
    * 
    * This call blocks until the Orc code publishes for the first time. If the call halts without
    * publishing this will return None.
    */
  def callOrcCallable(callable: AnyRef, arguments: List[AnyRef]): Option[AnyRef] = {
    Logger.fine(s"Calling from Java into Orc: $callable $arguments")
    val node = Call(Constant(callable), arguments map Constant, None)
    callNode(node)
  }

  /** Call a callable member of an Orc object with the given arguments and then return the first thing it publishes.
    * 
    * This call blocks until the Orc code publishes for the first time. If the call halts without
    * publishing this will return None.
    */
  def callOrcMethod(obj: AnyRef, field: Field, arguments: List[AnyRef]): Option[AnyRef] = {
    Logger.fine(s"Calling from Java into Orc: $obj$field $arguments")
    val node = FieldAccess(Constant(obj), field) >> Call(Variable(0), arguments map Constant, None)
    callNode(node)
  }

  private def callNode(node: orc.ast.oil.nameless.Expression): Option[AnyRef] = {
    val rootGroup = root.getOrElse { 
      throw new IllegalStateException("Cannot call into Orc because the root group does not exist (it has probably halted or been killed).")
    }
    val wrapper = new OrcCallWrapperGroup(rootGroup)
    val t = new Token(node, wrapper)
    schedule(t)
    val result = wrapper.await() 
    result
  }
}
