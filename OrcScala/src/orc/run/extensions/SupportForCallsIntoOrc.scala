//
// SupportForCallsIntoOrc.scala -- Scala trait SupportForCallsIntoOrc
// Project OrcScala
//
// Created by amp on Feb 11, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.ast.oil.nameless.{ Call, Constant, FieldAccess, Variable }
import orc.run.core.{ Group, Subgroup, Token }
import orc.values._

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
    while (!state.isInstanceOf[Bound]) {
      this.wait()
    }
    state match {
      case Bound(v) =>
        state = Completed
        v
      case Completed =>
        throw new AssertionError("The state is Completed at end of OrcCallWrapperGroup.await(). Await must have been called more than once.")
      case Unbound =>
        throw new AssertionError("The state is Unbound at end of OrcCallWrapperGroup.await().")
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
trait SupportForCallsIntoOrc {
  this: Group =>

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

  /** Call a callable member of an Orc object with the given arguments and then return the first thing it publishes.
    *
    * This call blocks until the Orc code publishes for the first time. If the call halts without
    * publishing this will return None.
    */
  def scheduleOrcMethod(obj: AnyRef, field: Field, arguments: List[AnyRef]): Unit = {
    val node = FieldAccess(Constant(obj), field) >> Call(Variable(0), arguments map Constant, None)
    scheduleNode(node)
  }

  private def callNode(node: orc.ast.oil.nameless.Expression): Option[AnyRef] = {
    val wrapper = startNode(node)
    val result = wrapper.await()
    result
  }

  private def startNode(node: orc.ast.oil.nameless.Expression) = {
    val wrapper = new OrcCallWrapperGroup(this)
    val t = new Token(node, wrapper)
    runtime.schedule(t)
    wrapper
  }

  private def scheduleNode(node: orc.ast.oil.nameless.Expression): Unit = {
    startNode(node)
  }
}
