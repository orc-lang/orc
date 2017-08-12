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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.util.ArrayList

/** A future value that can be bound or unbound or halted.
  *
  * Note: This implementation is from the Porc implementation, so
  * it is slightly more optimized than most of the token interpreter.
  * Specifically the states are Ints to avoid the need for objects
  * in the critical paths. The trade off is that Future contains an
  * extra couple of pointers.
  */
final class Future() extends OrcValue with orc.Future {
  import FutureConstants._

  // TODO: PERFORMANCE: Using an ArrayList here forces the use of a lock. Ideally we would avoid that. However, it would be quite a lot of work and may not gain much. Profile first.
  
  private var _state: AnyRef = Unbound
  private var _blocked: ArrayList[FutureReader] = new ArrayList()

  /** Bind this to a value and call publish and halt on each blocked Blockable.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def bind(v: AnyRef) = {
    //assert(!v.isInstanceOf[Field], s"Future cannot be bound to value $v")
    //assert(!v.isInstanceOf[orc.Future], s"Future cannot be bound to value $v")
    val done = synchronized {
      if (_state eq Unbound) {
        _state = v
        //Logger.finest(s"$this bound to $v")
        true
      } else {
        false
      }
    }
    // We can access and clear _blocked without the lock because we are in a
    // state that cannot change again.
    if (done) {
      _blocked.forEach(_.publish(v))
      _blocked = null
    }
  }

  /** Bind this to stop and call halt on each blocked Blockable.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def stop() = {
    val done = synchronized {
      if (_state eq Unbound) {
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
      _blocked.forEach(_.halt())
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
  @TruffleBoundary(allowInlining = true) @noinline
  def read(blocked: FutureReader): Unit = {
    val st = synchronized {
      _state match {
        case Unbound => {
          _blocked.add(blocked)
        }
        case _ => {}
      }
      _state
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

  override def toOrcSyntax() = {
    getInternal() match {
      case Unbound => "<$unbound$>"
      case Halt => "<$stop$>"
      case v => s"<${Format.formatValue(v)}>"
    }
  }

  def get(): FutureState = {
    getInternal() match {
      case Unbound => orc.FutureState.Unbound
      case Halt => orc.FutureState.Stopped
      case v => orc.FutureState.Bound(v)
    }
  }
  
  /** Get the state of this future.
   *  
   *  The state may change at any time. However, Unbound is the only state
   *  that is not stable, so if Halt or a value is returned it will not
   *  change.
   * 
   */
  def getInternal(): AnyRef = {
    _state
  }
}
