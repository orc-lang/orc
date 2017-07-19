//
// Future.scala -- Scala class Future
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porce.runtime

import orc.values.{ Format, OrcValue }
import orc.values.Field
import orc.FutureState
import orc.FutureReader

/** A future value that can be bound or unbound or halted.
  *
  * Note: This implementation is from the Porc implementation, so
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critical paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
final class Future() extends OrcValue with orc.Future {
  import Future._

  // TODO: PERFORMANCE: _value and _state could be combined by replacing the states with AnyRef objects and any other object is a bound value.
  //       Even _blocked could be combined in by having a specially wrapped List as the Unbound state. This would put allocations back on the some of the hot paths.
  var _state = Unbound
  var _value: AnyRef = null  
  // TODO: PERFORMANCE: An expanding array may perform quite a bit better since the adding a blocker would generally not need allocation (except when the array needs to expand).
  var _blocked: List[FutureReader] = Nil

  /** Bind this to a value and call publish and halt on each blocked Blockable.
    */
  def bind(v: AnyRef) = {
    assert(!v.isInstanceOf[Field], s"Future cannot be bound to value $v")
    assert(!v.isInstanceOf[orc.Future], s"Future cannot be bound to value $v")
    val done = synchronized {
      if (_state == Unbound) {
        _state = Bound
        _value = v
        //Logger.finest(s"$this bound to $v")
        true
      } else {
        false
      }
    }
    // We can access and clear _blocked without the lock because we are in a
    // state that cannot change again.
    if (done) {
      for (blocked <- _blocked) {
        blocked.publish(v)
      }
      _blocked = null
    }
  }

  /** Bind this to stop and call halt on each blocked Blockable.
    */
  def stop() = {
    val done = synchronized {
      if (_state == Unbound) {
        _state = Halt
        //Logger.finest(s"$this halted")
        true
      } else {
        false
      }
    }
    // We can access and clear _blocked without the lock because we are in a
    // state that cannot change again.
    if (done) {
      for (blocked <- _blocked) {
        blocked.halt()
      }
      _blocked = null
    }
  }

  /** Force this future into blocked.
    *
    * This may call publish in this thread if the future is already bound, or
    * if this is unbound, it adds blocked to the blockers list to be called
    * later.
    *
    * Return true if the value was already available.
    */
  def read(blocked: FutureReader): Unit = {
    val st = synchronized {
      _state match {
        case Unbound => {
          _blocked ::= blocked
        }
        case _ => {}
      }
      _state
    }

    st match {
      case Bound => {
        blocked.publish(_value)
      }
      case Halt => {
        blocked.halt()
      }
      case _ => {
      }
    }
  }

  override def toOrcSyntax() = {
    synchronized { _state } match {
      case Bound => s"<${Format.formatValue(_value)}>"
      case Halt => "<$stop$>"
      case Unbound => "<$unbound$>"
    }
  }

  def get(): FutureState = {
    synchronized { _state } match {
      case Unbound => orc.FutureUnbound
      case Bound => orc.FutureBound(_value)
      case Halt => orc.FutureStopped
    }
  }
}

object Future {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}
