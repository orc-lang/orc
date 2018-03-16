//
// Future.scala -- Scala class Future
// Project PorcE
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porce.runtime

import orc.{ FutureReader, FutureState }
import orc.values.{ Format, OrcValue }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/** A future value that can be bound or unbound or halted.
  *
  * This is very performance critical code. So it's hard to read. Sorry.
  * Try to comment optimizations carefully.
  */
class Future(val raceFreeResolution: Boolean) extends OrcValue with orc.Future {
  import FutureConstants._

  private var _state: AnyRef = Unbound
  private var _blockedIndex: Int = 0
  private var _blocked: Array[FutureReader] = Array.ofDim(2)

  /** Resolve this to a value and call publish and halt on each blocked FutureReader.
    */
  def bind(v: AnyRef) = {
    localBind(v)
  }

  /** Resolve this to stop and call halt on each blocked FutureReader.
    */
  def stop(): Unit = {
    localStop()
  }

  /** Resolve this to a value and call publish and halt on each blocked FutureReader.
    */
  //@TruffleBoundary(allowInlining = true) @noinline
  final def localBind(v: AnyRef): Unit = {
    //assert(!v.isInstanceOf[Field], s"Future cannot be bound to value $v")
    //assert(!v.isInstanceOf[orc.Future], s"Future cannot be bound to value $v")
    val finalBlocked = fastLocalBind(v)

    if (finalBlocked != null) {
      var i = 0
      while (i < finalBlocked.length && finalBlocked(i) != null) {
        finalBlocked(i).publish(v)
        i += 1
      }
    }
  }
  
  /** Resolve this to v, but return the future readers instead of calling publish on them.
	  */
  final def fastLocalBind(v: AnyRef): Array[FutureReader] = synchronized {
    if (_state eq Unbound) {
      _state = v
      val b = _blocked
      _blocked = null
      b
    } else {
      null
    }
  }

  /** Resolve this to stop and call halt on each blocked FutureReader.
    */
  //@TruffleBoundary(allowInlining = true) @noinline
  final def localStop(): Unit = {
    val finalBlocked = fastLocalStop()
    
    if (finalBlocked != null) {
      var i = 0
      while (i < finalBlocked.length && finalBlocked(i) != null) {
        finalBlocked(i).halt()
        i += 1
      }
    }
  }

  /** Resolve this to stop, but return the future readers instead of calling halt on them.
	  */
  final def fastLocalStop(): Array[FutureReader] = synchronized {
    if (_state eq Unbound) {
      _state = Halt
      val b = _blocked
      _blocked = null
      b
    } else {
      null
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
  @TruffleBoundary(allowInlining = true) @noinline
  final def read(blocked: FutureReader): Unit = {
    val st = {
      if (_state eq Unbound) synchronized {
        _state match {
          case Unbound => {
            // Extend array
            if (_blockedIndex == _blocked.length) {
              val oldBlocked = _blocked
              _blocked = Array.ofDim(_blocked.length * 2)
              System.arraycopy(oldBlocked, 0, _blocked, 0, oldBlocked.length)
            }
            // Add blocked to array
            _blocked(_blockedIndex) = blocked
            _blockedIndex += 1
          }
          case _ => {}
        }
        _state
      } else {
        _state
      }
    }

    st match {
      case Unbound => {
        // Don't do anything.
      }
      case Halt => {
        blocked.halt()
      }
      case v => {
        blocked.publish(v)
      }
    }
  }

  override def toOrcSyntax(): String = {
    _state match {
      case Unbound => "<$unbound$>"
      case Halt => "<$stop$>"
      case v => s"<${Format.formatValue(v)}>"
    }
  }

  /** Get the state of this future.
    *
    * The state may change at any time. However, Unbound is the only state
    * that is not stable, so if Halt or a value is returned it will not
    * change.
    */
  final def get: FutureState = {
    _state match {
      case Unbound => orc.FutureState.Unbound
      case Halt => orc.FutureState.Stopped
      case v => orc.FutureState.Bound(v)
    }
  }

  /** Get the state of this future, using the PorcE raw future status. */
  final def getInternal: AnyRef = {
    _state
  }

}
