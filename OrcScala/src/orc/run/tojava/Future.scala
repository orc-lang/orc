//
// Future.scala -- Scala class Future
// Project OrcScala
//
// $Id$
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
import orc.values.Format
import orc.values.OrcValue
import Future.Bound
import Future.Halt
import Future.Unbound

/** A future value that can be bound or unbound or halted.
  *
  * Note: This implementation is from the Porc implementation, so
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critical paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
final class Future(val runtime: OrcRuntime) extends OrcValue {
  import Future._

  var _state = Unbound
  var _value: AnyRef = null
  var _blocked: List[Blockable] = Nil

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
        blocked.halt()
      }
      _blocked = null
    }
  }

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
        blocked.halt()
      _blocked = null
    }
  }

  def forceIn(blocked: Blockable) = {
    val st = synchronized {
      _state match {
        case Unbound => {
          blocked.prepareSpawn()
          _blocked ::= blocked
        }
        case _ => {}
      }
      _state
    }

    st match {
      case Bound => blocked.publish(_value)
      case _ => {}
    }
  }

  override def toOrcSyntax() = {
    synchronized { _state } match {
      case Bound => Format.formatValue(_value)
      case Halt => "stop"
      case Unbound => "$unbound$"
    }
  }
}

object Future {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}
