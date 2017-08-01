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
    
  import Join._

  require(values.size > 1, "Join must have at least one argument. Check before call.")

  /**
   * The state of this join.
   * 
   * This encodes both the number of remaining values to finish and if this join has halted.
   * 
   * This should never be accessed directly. Use the update methods.
   */
  protected var state: Int = values.size - 1
  
  @inline
  protected def decrementUnboundST(): Unit = {
    state -= 1
  }
  
  @inline
  protected def setHaltedST(): Unit = {
    state = -1
  }
  
  @inline
  protected def setHaltedMT(): Boolean = {
    unsafe.getAndSetInt(this, joinStateOffset, -1) >= 0
  }
  
  @inline
  protected def isHaltedFN(): Boolean = {
    state < 0
  }
  
  @inline
  protected def isHaltedST(): Boolean = {
    state < 0
  }
  
  @inline
  protected def decrementUnboundMT(): Int = {
    unsafe.getAndAddInt(this, joinStateOffset, -1) - 1
  }
  
  @inline
  protected def getUnboundMT(): Int = {
    unsafe.getIntVolatile(this, joinStateOffset)
  }
  
  @inline
  protected def getUnboundST(): Int = {
    state
  }

  /**
   * A FutureReader that binds a specific element of values in publish().
   * 
   * All methods are called in the multi-threaded phase.
   */
  final private class JoinElement(i: Int, f: orc.Future) extends FutureReader {
    /**
     * The flag used to make sure publish/halt is only called once.
     */
    var bound = new AtomicBoolean(false)
    // TODO: PERFORMANCE: This could be replaces with a CAS directly into the values array, replacing this with the value.

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
      // Bind the value (this is not synchronized because checkComplete
      // will cause the read of it, enforcing the needed ordering).
      values(i + 1) = v
      // TODO: Does this write need to be volatile?
      // Now decrement the number of unbound values and see if we are done.
      join.checkComplete(decrementUnboundMT())
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

  /**
   * Add a future to force to the Join.
   * 
   * This should only be called in single-threaded mode.
   */
  @TruffleBoundary(allowInlining = true)
  def force(i: Int, f: orc.Future) = {
    //Logger.fine(s"Forcing $i $f ($state)")
    if (!isHaltedST()) {
      f.get() match {
        case orc.FutureState.Bound(v) => {
          set(i, v)
        }
        case orc.FutureState.Stopped => {
          setHaltedST()
        }
        case orc.FutureState.Unbound => {
          // Store a JoinElement in the array so it can be started later.
          values(i + 1) = new JoinElement(i, f)
        }
      }
    }
  }

  /**
   * Set a value to a specific value.
   * 
   * This should only be called in single-threaded mode.
   */
  def set(i: Int, v: AnyRef) = {
    //Logger.fine(s"Setting $i $v ($state)")
    // v is not a future so just bind it.
    values(i + 1) = v
    decrementUnboundST()
  }

  /**
   * Is this fully resolved to values.
   * 
   * This should only be called in single-threaded mode.
   */
  def isResolved() = {
    getUnboundST() == 0
  }

  /**
   * Is this halted because some value halted.
   * 
   * This should only be called in single-threaded mode.
   */
  def isHalted() = {
    isHaltedST()
  }

  /**
   * Is this waiting for a value.
   * 
   * This should only be called in single-threaded mode.
   */
  def isBlocked() = {
    getUnboundST() > 0
  }

  /**
   * Finish setting up this. After finish() is called this is in multi-threaded mode.
   * 
   * This should only be called in single-threaded mode.
   */
  @TruffleBoundary(allowInlining = true)
  def finish() = {
    //Logger.fine(s"Finishing $this with: $state ${values.mkString(", ")}")
    
    assert(isBlocked())
    
    unsafe.storeFence()

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
   * 
   * This may be called in multi-threaded mode.
   */
  private final def checkComplete(n: Int): Unit = {
    if (n == 0 && setHaltedMT()) {
      //Logger.finest(s"$join: Join finished with: ${values.mkString(", ")}")
      done()
    }
  }

  /**
   * Handle a successful completion.
   *
   * This should use values to get the bound values.
   * 
   * This may be called in multi-threaded mode.
   */
  def done(): Unit = {
    Logger.finer(s"Done for $this with: $state ${values.mkString(", ")}")
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
   * 
   * This may be called in multi-threaded mode.
   */
  def halt(): Unit = {
    if (setHaltedMT()) {
      Logger.finer(s"Halt for $state $this")
      t.removeChild(this)
      // Token: Remove token passed in.
      c.haltToken()
    }
  }

  /**
   * Handle being killed by the terminator.
   * 
   * This may be called in multi-threaded mode.
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
  
  val joinStateOffset = {
    unsafe.objectFieldOffset(classOf[Join].getDeclaredField("state"))
  }
}