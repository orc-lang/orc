//
// PorcNativeValues.scala -- Scala class/trait/object PorcNativeValues
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 15, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

final class Flag {
  @volatile
  var _value = false

  @inline
  def set() = {
    Logger.finest(s"Flag set $this")
    _value = true
  }
  @inline
  def get = _value
}

object Future {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}

final class Future {
  import Future._

  var _state = Unbound
  var _value: AnyRef = null
  var _blocked: List[Join] = Nil

  def value = {
    assert(_state == Bound)
    _value
  }

  def bind(v: AnyRef) = synchronized {
    if (_state == Unbound) {
      _state = Bound
      _value = v
      Logger.finest(s"$this bound to $v")
      for (j <- _blocked) {
        j.bindFuture()
      }
      _blocked = null
    }
  }
  def halt() = synchronized {
    if (_state == Unbound) {
      Logger.finest(s"$this halted")
      _state = Halt
      for (j <- _blocked) {
        j.haltFuture()
      }
      _blocked = null
    }
  }
  def addBlocked(j: Join) = synchronized {
    Logger.finest(s"${j} blocked on $this")
    _state match {
      case Unbound => {
        _blocked ::= j
      }
      case Bound => j.bindFuture()
      case Halt => j.haltFuture()
    }
  }
}

abstract class Join(fs: Map[Int, Future], terminator: Terminator) extends Terminable {
  var nToBind = fs.size // Less than 0 means halted

  def bindFuture() = synchronized {
    nToBind -= 1
    if (nToBind == 0) {
      Logger.finest(s"Completed join $this")
      if(terminator.removeTerminable(this))
        bound(fs.mapValues(_.value))
    }
  }
  def haltFuture() = synchronized {
    if (nToBind > 0) {
      Logger.finest(s"Halting join $this")
      nToBind = -1
      if(terminator.removeTerminable(this))
        halt()
    }
  }

  def kill() = haltFuture()

  def halt(): Unit
  def bound(nvs: Map[Int, AnyRef]): Unit
}
