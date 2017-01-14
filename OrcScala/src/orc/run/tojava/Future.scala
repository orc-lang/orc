//
// Future.scala -- Scala class Future
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.tojava

import orc.OrcRuntime
import orc.values.{ Format, OrcValue }

/** A future value that can be bound or unbound or halted.
  *
  * Note: This implementation is from the Porc implementation, so
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critical paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
final class Future() extends OrcValue {
  import Future._

  var _state = Unbound
  var _value: AnyRef = null
  var _blocked: List[Blockable] = Nil

  /** Bind this to a value and call publish and halt on each blocked Blockable.
    */
  def bind(v: AnyRef) = {
    assert(!v.isInstanceOf[Future])
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
        blocked.halt() // Matched to: prepareSpawn in forceIn
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
      for (blocked <- _blocked)
        blocked.halt() // Matched to: prepareSpawn in forceIn
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
  def forceIn(blocked: Blockable): Boolean = {
    val st = synchronized {
      _state match {
        case Unbound => {
          blocked.prepareSpawn() // Matched to: halt in bind and stop.
          _blocked ::= blocked
        }
        case _ => {}
      }
      _state
    }

    st match {
      case Bound => {
        blocked.publish(_value)
        true
      }
      case Halt => {
        // If the state was Halt then just return without publishing.
        // We never prepareSpawn'd so we don't need to halt.
        true
      }
      case _ => {
        false
      }
    }
  }

  override def toOrcSyntax() = {
    synchronized { _state } match {
      case Bound => Format.formatValue(_value)
      case Halt => "$stop$"
      case Unbound => "$unbound$"
    }
  }
}

object Future {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}
