//
// Join.scala -- Scala classes Join and Resolve
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import orc.FutureReader
import orc.run.porce.Logger
import sun.misc.Unsafe
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

/**
 * Join a number of futures by blocking on all of them simultaneously.
 *
 * This is analogous to orc.run.core.Join.
 *
 * @param inValues a list of value which may contain futures. All the futures in this array will be blocked on.
 *
 * This class must be subclassed to implement halt and done.
 *
 * @author amp
 */
final class Join(val p: PorcEClosure, val c: Counter, val t: Terminator, val values: Array[AnyRef], runtime: PorcERuntime) extends Terminatable {
  join =>

  require(values.size > 1, "Join must have at least one argument. Check before call.")

  // The number of unbound values in values.
  protected var nUnbound = new AtomicInteger(values.size - 1)
  // The flag saying if we have already halted.
  protected val halted = new AtomicBoolean(false)

  /**
   * A FutureReader that binds a specific element of values in publish().
   */
  final private class JoinElement(i: Int, f: orc.Future) extends FutureReader {
    /**
     * The flag used to make sure publish/halt is only called once.
     */
    var bound = new AtomicBoolean(false)

    /**
     * Start blocking on the future.
     *
     */
    def start() = {
      f.read(this)
    }

    /**
     * Bind value i to v if this is not yet bound.
     *
     * If join has not halted we bind and check if we are done.
     */
    def publish(v: AnyRef): Unit = if (bound.compareAndSet(false, true)) {
      //Logger.finest(s"$join: Join joined to $v ($i)")
      // Check if we are halted then bind. This is an optimization since
      // nUnbound can never reach 0 if we halted.
      if (!join.halted.get()) {
        // Bind the value (this is not synchronized because checkComplete
        // will cause the read of it, enforcing the needed ordering).
        values(i + 1) = v
        // TODO: Does this write need to be volatile?
        // Now decrement the number of unbound values and see if we are done.
        join.checkComplete(join.nUnbound.decrementAndGet())
      }
    }

    /**
     * Halt the whole join if it has not yet been halted and this has not yet
     * been bound.
     */
    def halt(): Unit = if (bound.compareAndSet(false, true)) {
      //Logger.finest(s"$join: Join halted ($i)")
      // Halt if we have not already halted.
      join.halt()
    }
  }

  @TruffleBoundary(allowInlining = true)
  def force(i: Int, f: orc.Future) = {
    //Logger.fine(s"Forcing $i $f")
    if (!join.halted.get()) {
      f.get() match {
        case orc.FutureState.Bound(v) => {
          set(i, v)
        }
        case orc.FutureState.Stopped => {
          halted.set(true)
        }
        case orc.FutureState.Unbound => {
          // Store a JoinElement in the array so it can be started later.
          values(i + 1) = new JoinElement(i, f)
        }
      }
    }
  }

  def set(i: Int, v: AnyRef) = {
    //Logger.fine(s"Setting $i $v")
    // v is not a future so just bind it.
    values(i + 1) = v
    nUnbound.decrementAndGet()
  }

  def isResolved() = {
    nUnbound.get() == 0
  }

  def isHalted() = {
    halted.get()
  }

  def isBlocked() = {
    nUnbound.get() > 0 && !halted.get()
  }

  @TruffleBoundary(allowInlining = true)
  def finish() = {
    //Logger.fine(s"Finishing $this with: ${values.mkString(", ")}")
    
    assert(isBlocked())

    t.addChild(this)

    var i = 1
    while (i < values.length) {
      values(i) match {
        case p: JoinElement =>
          p.start()
        case _ =>
          {}
      }
      i += 1
    }
  }

  /**
   * Check if we are done by looking at n which must be the current number of
   * unbound values.
   */
  private final def checkComplete(n: Int): Unit = {
    assert(n >= 0, n)
    if (n == 0 && halted.compareAndSet(false, true)) {
      //Logger.finest(s"$join: Join finished with: ${values.mkString(", ")}")
      done()
    }
  }

  /**
   * Handle a successful completion.
   *
   * This should use values to get the bound values.
   */
  def done(): Unit = {
    Logger.finer(s"Done for $this with: ${values.mkString(", ")}")
    t.removeChild(this)
    // Token: Pass to p.
    runtime.scheduleOrCall(c, () => { 
      p.callFromRuntimeArgArray(values)
    })
  }

  /**
   * Handle a halting case.
   *
   * values will not be completely bound.
   */
  def halt(): Unit = {
    if (halted.compareAndSet(false, true)) {
      Logger.finer(s"Halt for $this")
      t.removeChild(this)
      // Token: Remove token passed in.
      c.haltToken()
    }
  }

  /**
   * Handle being killed by the terminator.
   */
  def kill(): Unit = {
    // This join has been killed
    halt()
  }
}

object Join {
  val unsafe = {
    val theUnsafe = classOf[Unsafe].getDeclaredField("theUnsafe");
    theUnsafe.setAccessible(true);
    theUnsafe.get(null).asInstanceOf[Unsafe];  
  }
}