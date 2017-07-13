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
package orc.run.core

import orc.OrcRuntime
import orc.values.{ Format, OrcValue }
import orc.FutureState
import orc.FutureReader

/** Interface for futures.
  *
  * This exists to provide a common interface for LocalFuture and
  * RemoteFuture if they need to have unrelated implementations.
  */
trait Future extends ReadableBlocker with OrcValue with orc.Future {
  def bind(v: AnyRef): Unit
  def stop(): Unit
}

/** A future value that can be bound or unbound or halted.
  *
  * Note: This implementation is from the Porc implementation, so
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critical paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
class LocalFuture(val runtime: OrcRuntime) extends Future {
  import LocalFuture._

  var _state = Unbound
  var _value: AnyRef = null
  var _blocked: List[Either[Blockable, FutureReader]] = Nil

  def bind(v: AnyRef) = {
    val (didIt, st) = synchronized {
      if (_state == Unbound) {
        _state = Bound
        _value = v
        (true, _state)
      } else {
        (false, _state)
      }
    }
    if (didIt) {
      //Logger.finest(s"$this bound to $v")
      scheduleBlocked(st)
    }
  }
    
  def stop() = {
    val (didIt, st) = synchronized {
      if (_state == Unbound) {
        //Logger.finest(s"$this halted")
        _state = Halt
        (true, _state)
      } else {
        (false, _state)
      }
    }
    if (didIt) {
      scheduleBlocked(st)
    }
  }

  private def scheduleBlocked(st: Int): Unit = {
    assert(st != Unbound)
    
    for (j <- _blocked) {
      j match {
        case Left(j) =>
          runtime.schedule(j)
        case Right(j) =>
          st match {
            case Bound => j.publish(_value)
            case Halt => j.halt()
          }
      }
    }
    // We will never need the blocked list again, so clear it to allow the blockables to be collected
    _blocked = null
  }

  def read(blockable: Blockable) = {
    val st = synchronized {
      _state match {
        case Unbound => {
          blockable.blockOn(this)
          _blocked ::= Left(blockable)
        }
        case _ => {}
      }
      _state
    }

    st match {
      case Bound => blockable.awakeTerminalValue(_value)
      case Halt => blockable.awakeStop()
      case Unbound => {}
    }
  }

  override def check(blockable: Blockable): Unit = orc.util.Profiler.measureInterval(0L, 'LocalFuture_check) {
    synchronized { _state } match {
      case Unbound => throw new AssertionError("Spurious call to Future.check.")
      case Bound => blockable.awakeTerminalValue(_value)
      case Halt => blockable.awakeStop()
    }
  }

  // TODO: Replace this with the get() method from orc.Future. We could rename get() if desired.
  def readIfResolved() = {
    synchronized { _state } match {
      case Unbound => None
      case Bound => Some(Some(_value))
      case Halt => Some(None)
    }
  }

  override def toOrcSyntax() = {
    synchronized { _state } match {
      case Bound => Format.formatValue(_value)
      case Halt => "stop"
      case Unbound => "$unbound$"
    }
  }

  // orc.Future API
  
  def get(): FutureState = {
    synchronized { _state } match {
      case Unbound => orc.FutureUnbound
      case Bound => orc.FutureBound(_value)
      case Halt => orc.FutureStopped
    }
  }

  def read(reader: FutureReader): Unit = {
    val st = synchronized {
      _state match {
        case Unbound => {
          _blocked ::= Right(reader)
        }
        case _ => {}
      }
      _state
    }

    st match {
      case Bound => reader.publish(_value)
      case Halt => reader.halt()
      case Unbound => {}
    }
  }
}

private object LocalFuture {
  val Unbound = 0
  val Bound = 1
  val Halt = 2
}
