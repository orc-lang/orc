//
// Resolver.scala -- Scala class Resolver
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

import orc.run.porce.SimpleWorkStealingSchedulerWrapper

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import sun.misc.Unsafe

// TODO: Try to remove redundency between this and Join.

/** Join a number of futures by blocking on all of them simultaneously waiting for them to ALL either be
  * bound or halt.
  *
  * @param inValues a list of value which may contain futures. All the futures in this array will be blocked on.
  *
  * This class must be subclassed to implement done.
  *
  * @author amp
  */
final class Resolver(val p: PorcEClosure, val c: Counter, val t: Terminator, val nValues: Int, execution: PorcEExecution) extends Terminatable {
  resolver =>

  import Resolver._

  SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)

  //require(nValues > 0, "Join must have at least one argument. Check before call.")

  private var elements: ArrayList[JoinElement] = new ArrayList()

  /** The state of this join.
    *
    * This encodes both the number of remaining values to finish.
    *
    * This should never be accessed directly. Use the update methods.
    */
  protected var state: Int = nValues

  @inline
  protected def decrementUnboundST(): Unit = {
    state -= 1
  }

  @inline
  protected def decrementUnboundMT(): Int = {
    unsafe.getAndAddInt(this, resolverStateOffset, -1) - 1
  }

  @inline
  protected def setKilledMT(): Boolean = {
    unsafe.getAndAddInt(this, resolverStateOffset, -1) > 0
  }

  @inline
  protected def getUnboundMT(): Int = {
    unsafe.getIntVolatile(this, resolverStateOffset)
  }

  @inline
  protected def getUnboundST(): Int = {
    state
  }

  /** A FutureReader that binds a specific element of values in publish().
    *
    * All methods are called in the multi-threaded phase.
    */
  final private class JoinElement(i: Int, f: orc.Future) extends AtomicBoolean with PorcEFutureReader {
    /** Start blocking on the future.
      *
      */
    def start() = {
      f.read(this)
    }

    /** Bind value i to v if this is not yet bound.
      *
      * If join has not halted we bind and check if we are done.
      */
    def publish(v: AnyRef): Unit = halt()

    /** Halt the whole join if it has not yet been halted and this has not yet
      * been bound.
      */
    def halt(): Unit = {
      if (compareAndSet(false, true)) {
        //Logger.finest(s"$join: Join halted ($i)")
        // Halt if we have not already halted.
        SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)
        resolver.checkComplete(decrementUnboundMT())
      }
    }

    def fastPublish(v: AnyRef): CallClosureSchedulable = {
      val p = fastHalt()
      if (p != null) {
        val s = CallClosureSchedulable(p, execution)
        SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
        s
      } else {
        null
      }
    }

    def fastHalt(): PorcEClosure = {
      if (compareAndSet(false, true)) {
        // Now decrement the number of unbound values and see if we are done.
        SimpleWorkStealingSchedulerWrapper.traceTaskParent(SimpleWorkStealingSchedulerWrapper.currentSchedulable, this)
        resolver.fastCheckComplete(decrementUnboundMT())
      } else {
        null
      }
    }
  }

  /** Add a future to force to the Join.
    *
    * This should only be called in single-threaded mode.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def force(i: Int, f: orc.run.porce.runtime.Future) = {
    //Logger.fine(s"Forcing $i $f ($state)")
    f.getInternal match {
      case FutureConstants.Unbound => {
        // Store a JoinElement in the array so it can be started later.
        elements.add(new JoinElement(i, f))
      }
      case v => {
        // Handle both values and halted.
        set(i)
      }
    }
  }

  /** Add a future to force to the Join.
    *
    * This should only be called in single-threaded mode.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def force(i: Int, f: orc.Future) = {
    //Logger.fine(s"Forcing $i $f ($state)")
    f.get match {
      case FutureConstants.Orc_Unbound => {
        // Store a JoinElement in the array so it can be started later.
        elements.add(new JoinElement(i, f))
      }
      case _ => {
        set(i)
      }
    }
  }

  /** Set a value to a specific value.
    *
    * This should only be called in single-threaded mode.
    */
  def set(i: Int) = {
    decrementUnboundST()
  }

  /** Is this fully resolved to values.
    *
    * This should only be called in single-threaded mode.
    */
  def isResolved() = {
    getUnboundST() == 0
  }

  /** Is this waiting for a value.
    *
    * This should only be called in single-threaded mode.
    */
  def isBlocked() = {
    getUnboundST() > 0
  }

  /** Finish setting up this. After finish() is called this is in multi-threaded mode.
    *
    * This should only be called in single-threaded mode.
    */
  @TruffleBoundary @noinline
  def finish() = {
    if(isBlocked()) {
      // Force flushes because p could be called in another thread at any time.
      Counter.flushAllCounterOffsets(1)
      finishBlocked()
    } else {
      unsafe.fullFence()
      t.addChild(this)
      checkComplete(0)
    }

    elements = null
  }

  /** As finish(), but this must be in the blocked state.
    *
    * This is slightly faster and has smaller code-size than finish().
    */
  @TruffleBoundary @noinline
  def finishBlocked() = {
    // This fence is needed because we are doing non-atomic accesses before this call, but
    // after this we may have updates or reads from other threads.
    unsafe.fullFence()

    t.addChild(this)

    var i = 0
    while (i < elements.size()) {
      elements.get(i) match {
        case p: JoinElement =>
          p.start()
        case _ =>
          {}
      }
      i += 1
    }

    elements = null
  }

  /** Check if we are done by looking at n which must be the current number of
    * unbound values.
    *
    * This may be called in multi-threaded mode.
    */
  private final def checkComplete(n: Int): Unit = {
    if (n == 0) {
      //Logger.finest(s"$join: Join finished with: ${values.mkString(", ")}")
      done()
    }
  }

  private final def fastCheckComplete(n: Int): PorcEClosure = {
    if (n == 0) {
      //Logger.finest(s"$join: Join finished with: ${values.mkString(", ")}")
      fastDone()
    } else {
      null
    }
  }

  /** Handle a successful completion.
    */
  @TruffleBoundary(allowInlining = true) @noinline
  def done(): Unit = {
    val s = CallClosureSchedulable(fastDone(), execution)
    SimpleWorkStealingSchedulerWrapper.shareSchedulableID(s, this)
    execution.runtime.potentiallySchedule(s)
  }

  def fastDone(): PorcEClosure = {
    t.removeChild(this)
    // Token: Pass to p.
    p
  }

  /** Handle being killed by the terminator.
    *
    * This may be called in multi-threaded mode.
    */
  def kill(): Unit = {
    // This join has been killed
    if (setKilledMT()) {
      t.removeChild(this)
      // Token: Remove token passed in.
      c.haltToken()
    }
  }
}

object Resolver {
  val unsafe = {
    val theUnsafe = classOf[Unsafe].getDeclaredField("theUnsafe");
    theUnsafe.setAccessible(true);
    theUnsafe.get(null).asInstanceOf[Unsafe];
  }

  val resolverStateOffset = {
    unsafe.objectFieldOffset(classOf[Resolver].getDeclaredField("state"))
  }
}
